package shifter.cache.memcached

import internals._
import shifter.cache._
import concurrent.{Future, ExecutionContext}
import net.spy.memcached.{FailureMode => SpyFailureMode, _}
import net.spy.memcached.ConnectionFactoryBuilder.{Protocol => SpyProtocol}
import net.spy.memcached.auth.{PlainCallbackHandler, AuthDescriptor}
import concurrent.duration._
import java.util.concurrent.TimeUnit


/**
 * Memcached client implementation based on SpyMemcached.
 *
 * See the parent trait (Cache) for API docs.
 */
trait Memcached extends Cache {
  protected def config: Configuration

  def asyncAdd[T](key: String, value: T, exp: Duration = defaultExpiry)(implicit ec: ExecutionContext): Future[Boolean] =
    if (value != null)
      instance.realAsyncAdd(withPrefix(key), value, exp, config.operationTimeout) map {
        case SuccessfulResult(givenKey, Some(_)) =>
          true
        case SuccessfulResult(givenKey, None) =>
          false
        case failure: FailedResult =>
          throwExceptionOn(failure)
      }
    else
      Future.successful(false)

  def asyncSet[T](key: String, value: T, exp: Duration = defaultExpiry)(implicit ec: ExecutionContext): Future[Unit] = {
    if (value != null)
      instance.realAsyncSet(withPrefix(key), value, exp, config.operationTimeout) map {
        case SuccessfulResult(givenKey, _) =>
          ()
        case failure: FailedResult =>
          throwExceptionOn(failure)
      }
    else
      Future.successful(())
  }

  def asyncDelete(key: String)(implicit ec: ExecutionContext): Future[Boolean] =
    instance.realAsyncDelete(withPrefix(key), config.operationTimeout) map {
      case SuccessfulResult(givenKey, result) =>
        result
      case failure: FailedResult =>
        throwExceptionOn(failure)
    }

  def apply[T](key: String)(implicit ec: ExecutionContext): Future[T] =
    instance.realAsyncGet[T](withPrefix(key), config.operationTimeout) map {
      case SuccessfulResult(givenKey, Some(value)) =>
        value
      case SuccessfulResult(givenKey, None) =>
        throw new KeyNotInCacheException(withoutPrefix(givenKey))
      case failure: FailedResult =>
        throwExceptionOn(failure)
    }

  def asyncGet[T](key: String)(implicit ec: ExecutionContext): Future[Option[T]] =
    instance.realAsyncGet[T](withPrefix(key), config.operationTimeout) map {
      case SuccessfulResult(givenKey, option) =>
        option
      case failure: FailedResult =>
        throwExceptionOn(failure)
    }

  def getOrElse[T](key: String, default: => T)(implicit ec: ExecutionContext): Future[T] =
    asyncGet[T](key) map {
      case Some(value) => value
      case None => default
    }

  def asyncBulk(keys: Traversable[String])(implicit ec: ExecutionContext): Future[Map[String, Any]] = {
    val givenKeys = keys.toSet
    val futures = givenKeys.map(key => instance.realAsyncGet[Any](withPrefix(key), config.operationTimeout))
    val bulkQuery = Future.sequence(futures)

    bulkQuery.map{ list =>
      val map = list.map {
        case SuccessfulResult(prefixedKey, Some(value)) =>
          val key = withoutPrefix(prefixedKey)
          Some((key, value))
        case SuccessfulResult(_, None) =>
          None
        case failure: FailedResult =>
          throwExceptionOn(failure)
      }

      map.flatten.toMap
    }
  }

  def cas[T](key: String, expecting: Option[T], newValue: T, exp: Duration = defaultExpiry)(implicit ec: ExecutionContext): Future[Boolean] =
    expecting match {
      case None =>
        asyncAdd[T](key, newValue, exp)

      case Some(expectingValue) =>
        instance.realAsyncGets[T](withPrefix(key), config.operationTimeout) flatMap {
          case SuccessfulResult(givenKey, None) =>
            Future.successful(false)

          case SuccessfulResult(givenKey, Some((value, casID))) =>
            if (value == expectingValue) {
              instance.realAsyncCAS(withPrefix(key), casID, newValue, exp, config.operationTimeout) map {
                case SuccessfulResult(_, bool) =>
                  bool
                case failure: FailedResult =>
                  throwExceptionOn(failure)
              }
            }
            else
              Future.successful(false)

          case failure: FailedResult =>
            throwExceptionOn(failure)
        }
    }

  def transformAndGet[T](key: String, exp: Duration = defaultExpiry)(cb: Option[T] => T)(implicit ec: ExecutionContext): Future[T] = {
    val keyWithPrefix = withPrefix(key)

    def loop(retry: Int): Future[T] =
      instance.realAsyncGets[T](keyWithPrefix, config.operationTimeout).flatMap {
        case SuccessfulResult(_, None) =>
          val result = cb(None)
          asyncAdd(key, result, exp) flatMap {
            case true =>
              Future.successful(result)
            case false =>
              loop(retry + 1)
          }
        case SuccessfulResult(_, Some((current, casID))) =>
          val result = cb(Some(current))

          instance.realAsyncCAS(keyWithPrefix, casID, result, exp, config.operationTimeout) flatMap {
            case SuccessfulResult(_, true) =>
              Future.successful(result)
            case SuccessfulResult(_, false) =>
              if (config.maxTransformCASRetries > 0 && retry < config.maxTransformCASRetries)
                loop(retry + 1)
              else
                instance.realAsyncSet(keyWithPrefix, result, exp, config.operationTimeout).map(_ => result)
            case failure: FailedResult =>
              throwExceptionOn(failure)
          }

        case failure: FailedResult =>
          throwExceptionOn(failure)
      }

    loop(0)
  }

  def getAndTransform[T](key: String, exp: Duration = defaultExpiry)(cb: Option[T] => T)(implicit ec: ExecutionContext): Future[Option[T]] = {
    val keyWithPrefix = withPrefix(key)

    def loop(retry: Int): Future[Option[T]] =
      instance.realAsyncGets[T](keyWithPrefix, config.operationTimeout).flatMap {
        case SuccessfulResult(_, None) =>
          val result = cb(None)
          asyncAdd(key, result, exp) flatMap {
            case true =>
              Future.successful(None)
            case false =>
              loop(retry + 1)
          }

        case SuccessfulResult(_, Some((current, casID))) =>
          val result = cb(Some(current))

          instance.realAsyncCAS(keyWithPrefix, casID, result, exp, config.operationTimeout) flatMap {
            case SuccessfulResult(_, true) =>
              Future.successful(Some(current))
            case SuccessfulResult(_, false) =>
              if (config.maxTransformCASRetries > 0 && retry < config.maxTransformCASRetries)
                loop(retry + 1)
              else
                instance.realAsyncSet(keyWithPrefix, result, exp, config.operationTimeout).map(_ => Some(current))
            case failure: FailedResult =>
              throwExceptionOn(failure)
          }

        case failure: FailedResult =>
          throwExceptionOn(failure)
      }

    loop(0)
  }

  def shutdown() {
    instance.shutdown(3, TimeUnit.SECONDS)
  }

  private[this] def throwExceptionOn(failure: FailedResult) = failure match {
    case FailedResult(k, TimedOutStatus) =>
      throw new TimeoutException(withoutPrefix(k))
    case FailedResult(k, CancelledStatus) =>
      throw new TimeoutException(withoutPrefix(k))
    case FailedResult(k, unhandled) =>
      throw new UnhandledStatusException(
        "For key %s: %s".format(withoutPrefix(k), unhandled.getClass.getName))
  }

  @inline
  private[this] def withPrefix(key: String): String =
    if (prefix.isEmpty)
      key
    else
      prefix + "-" + key

  @inline
  private[this] def withoutPrefix[T](key: String): String = {
    if (!prefix.isEmpty && key.startsWith(prefix + "-"))
      key.substring(prefix.length + 1)
    else
      key
  }

  private[this] val prefix = config.keysPrefix.getOrElse("")
  private[this] val instance = {
    System.setProperty("net.spy.log.LoggerImpl",
      "shifter.cache.memcached.internals.Slf4jLogger")

    val conn = {
      val builder = new ConnectionFactoryBuilder()
        .setTranscoder(config.transcoder.getOrElse(new CustomTranscoder))
        .setProtocol(
          if (config.protocol == Protocol.Binary)
            SpyProtocol.BINARY
          else
            SpyProtocol.TEXT
        )
        .setDaemon(true)
        .setFailureMode(config.failureMode match {
          case FailureMode.Retry =>
            SpyFailureMode.Retry
          case FailureMode.Cancel =>
            SpyFailureMode.Cancel
          case FailureMode.Redistribute =>
            SpyFailureMode.Redistribute
        })

      val withTimeout = config.operationTimeout match {
        case duration: FiniteDuration =>
          builder.setOpTimeout(config.operationTimeout.toMillis)
        case _ =>
          builder
      }

      val withAuth = config.authentication match {
        case Some(credentials) =>
          withTimeout.setAuthDescriptor(
            new AuthDescriptor(Array("PLAIN"),
              new PlainCallbackHandler(credentials.username, credentials.password)))
        case None =>
          withTimeout
      }

      withAuth
    }

    val addresses = AddrUtil.getAddresses(config.addresses)
    new SpyMemcachedClient(conn.build(), addresses)
  }
}

object Memcached {
  def apply(cfg: Configuration)(implicit ec: ExecutionContext) =
    new Memcached {
      protected def config = cfg
    }
}
