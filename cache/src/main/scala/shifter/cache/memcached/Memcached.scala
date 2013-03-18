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

  def add[T](key: String, value: T, exp: Duration = defaultExpiry)(implicit ec: ExecutionContext): Future[Boolean] =
    instance.realAsyncAdd(withPrefix(key), value, exp) map {
      case SuccessfulResult(givenKey, Some(_)) =>
        assert(givenKey == withPrefix(key), "wrong key returned: " + givenKey)
        true
      case SuccessfulResult(givenKey, None) =>
        assert(givenKey == withPrefix(key), "wrong key returned: " + givenKey)
        false
      case FailedResult(_, TimedOutStatus) =>
        throw new TimeoutException
      case FailedResult(_, CancelledStatus) =>
        throw new TimeoutException
      case FailedResult(_, failure) =>
        throw new UnhandledStatusException(failure.getClass.getName)
    }


  def set[T](key: String, value: T, exp: Duration = defaultExpiry)(implicit ec: ExecutionContext): Future[Unit] =
    instance.realAsyncSet(withPrefix(key), value, exp) map {
      case SuccessfulResult(givenKey, _) =>
        assert(givenKey == withPrefix(key), "wrong key returned: " + givenKey)
        ()
      case FailedResult(_, TimedOutStatus) =>
        throw new TimeoutException
      case FailedResult(_, CancelledStatus) =>
        throw new TimeoutException
      case FailedResult(_, failure) =>
        throw new UnhandledStatusException(failure.getClass.getName)
    }


  def delete(key: String)(implicit ec: ExecutionContext): Future[Boolean] =
    instance.realDelete(withPrefix(key)) map {
      case SuccessfulResult(givenKey, result) =>
        assert(givenKey == withPrefix(key), "wrong key returned: " + givenKey)
        result
      case FailedResult(_, TimedOutStatus) =>
        throw new TimeoutException
      case FailedResult(_, CancelledStatus) =>
        throw new TimeoutException
      case FailedResult(_, failure) =>
        throw new UnhandledStatusException(failure.getClass.getName)
    }

  def apply[T](key: String)(implicit ec: ExecutionContext): Future[T] =
    instance.realAsyncGet[T](withPrefix(key)) map {
      case SuccessfulResult(givenKey, Some(value)) =>
        assert(givenKey == withPrefix(key), "wrong key returned: " + givenKey)
        value
      case SuccessfulResult(givenKey, None) =>
        assert(givenKey == withPrefix(key), "wrong key returned: " + givenKey)
        throw new KeyNotInCacheException("memcached." + withoutPrefix(givenKey))
      case FailedResult(_, TimedOutStatus) =>
        throw new TimeoutException
      case FailedResult(_, CancelledStatus) =>
        throw new TimeoutException
      case FailedResult(_, failure) =>
        throw new UnhandledStatusException(failure.getClass.getName)
    }

  def get[T](key: String)(implicit ec: ExecutionContext): Future[Option[T]] =
    instance.realAsyncGet[T](withPrefix(key)) map {
      case SuccessfulResult(givenKey, option) =>
        assert(givenKey == withPrefix(key), "wrong key returned: " + givenKey)
        option
      case FailedResult(_, TimedOutStatus) =>
        throw new TimeoutException
      case FailedResult(_, CancelledStatus) =>
        throw new TimeoutException
      case FailedResult(_, failure) =>
        throw new UnhandledStatusException(failure.getClass.getName)
    }

  def getOrElse[T](key: String, default: => T)(implicit ec: ExecutionContext): Future[T] =
    get[T](key) map {
      case Some(value) => value
      case None => default
    }

  def getBulk(keys: Traversable[String])(implicit ec: ExecutionContext): Future[Map[String, Any]] = {
    val givenKeys = keys.toSet
    val futures = givenKeys.map(key => instance.realAsyncGet[Any](withPrefix(key)))
    val bulkQuery = Future.sequence(futures)

    bulkQuery.map{ list =>
      val map = list.map {
        case SuccessfulResult(prefixedKey, Some(value)) =>
          val key = withoutPrefix(prefixedKey)
          assert(givenKeys(key), "wrong key returned (%s)".format(key))
          Some((key, value))
        case SuccessfulResult(_, None) =>
          None
        case FailedResult(_, TimedOutStatus) =>
          throw new TimeoutException
        case FailedResult(_, CancelledStatus) =>
          throw new TimeoutException
        case FailedResult(_, failure) =>
          throw new UnhandledStatusException(failure.getClass.getName)
      }

      map.flatten.toMap
    }
  }

  def cas[T](key: String, expecting: Option[T], newValue: T, exp: Duration = defaultExpiry)(implicit ec: ExecutionContext): Future[Boolean] =
    expecting match {
      case None =>
        add[T](key, newValue, exp)
      case Some(expectingValue) =>
        instance.realAsyncGets[T](withPrefix(key)) flatMap {
          case SuccessfulResult(givenKey, None) =>
            Future.successful(false)

          case SuccessfulResult(givenKey, Some((value, casID))) =>
            if (value == expectingValue) {
              instance.realAsyncCAS(withPrefix(key), casID, newValue, exp) map {
                case SuccessfulResult(_, bool) =>
                  bool
                case FailedResult(_, TimedOutStatus) =>
                  throw new TimeoutException
                case FailedResult(_, CancelledStatus) =>
                  throw new TimeoutException
                case FailedResult(_, failure) =>
                  throw new UnhandledStatusException(failure.getClass.getName)
              }
            }
            else
              Future.successful(false)

          case FailedResult(_, TimedOutStatus) =>
            throw new TimeoutException
          case FailedResult(_, CancelledStatus) =>
            throw new TimeoutException
          case FailedResult(_, failure) =>
            throw new UnhandledStatusException(failure.getClass.getName)
        }
    }

  def transformAndGet[T](key: String, exp: Duration = defaultExpiry)(cb: Option[T] => T)(implicit ec: ExecutionContext): Future[T] =
    instance.realAsyncGets[T](withPrefix(key)).flatMap {
      case SuccessfulResult(_, None) =>
        val result = cb(None)
        add(key, result, exp) flatMap {
          case true =>
            Future.successful(result)
          case false =>
            transformAndGet[T](key, exp)(cb)
        }
      case SuccessfulResult(_, Some((current, casID))) =>
        val result = cb(Some(current))

        instance.realAsyncCAS(withPrefix(key), casID, result, exp) flatMap {
          case SuccessfulResult(_, true) =>
            Future.successful(result)
          case SuccessfulResult(_, false) =>
            transformAndGet[T](key, exp)(cb)
          case FailedResult(_, TimedOutStatus) =>
            throw new TimeoutException
          case FailedResult(_, CancelledStatus) =>
            throw new TimeoutException
          case FailedResult(_, failure) =>
            throw new UnhandledStatusException(failure.getClass.getName)
        }

      case FailedResult(_, TimedOutStatus) =>
        throw new TimeoutException
      case FailedResult(_, CancelledStatus) =>
        throw new TimeoutException
      case FailedResult(_, failure) =>
        throw new UnhandledStatusException(failure.getClass.getName)
    }

  def getAndTransform[T](key: String, exp: Duration = defaultExpiry)(cb: Option[T] => T)(implicit ec: ExecutionContext): Future[Option[T]] =
    instance.realAsyncGets[T](withPrefix(key)).flatMap {
      case SuccessfulResult(_, None) =>
        val result = cb(None)
        add(key, result, exp) flatMap {
          case true =>
            Future.successful(None)
          case false =>
            getAndTransform[T](key, exp)(cb)
        }

      case SuccessfulResult(_, Some((current, casID))) =>
        val result = cb(Some(current))

        instance.realAsyncCAS(withPrefix(key), casID, result, exp) flatMap {
          case SuccessfulResult(_, true) =>
            Future.successful(Some(current))
          case SuccessfulResult(_, false) =>
            getAndTransform[T](key, exp)(cb)
          case FailedResult(_, TimedOutStatus) =>
            throw new TimeoutException
          case FailedResult(_, CancelledStatus) =>
            throw new TimeoutException
          case FailedResult(_, failure) =>
            throw new UnhandledStatusException(failure.getClass.getName)
        }

      case FailedResult(_, TimedOutStatus) =>
        throw new TimeoutException
      case FailedResult(_, CancelledStatus) =>
        throw new TimeoutException
      case FailedResult(_, failure) =>
        throw new UnhandledStatusException(failure.getClass.getName)
    }

  def shutdown() {
    instance.shutdown(3, TimeUnit.SECONDS)
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
    val conn = {
      val builder = new ConnectionFactoryBuilder()
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
