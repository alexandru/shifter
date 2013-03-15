package shifter.cache

import concurrent.{ExecutionContext, Future}
import errors.{CacheError, NotFoundInCacheError, CacheClientNotRunning}
import interop.{CAS_EXISTS, CAS_NOT_FOUND, CAS_OK}
import java.util.concurrent.TimeUnit
import net.spy.memcached.{FailureMode => SpyFailureMode, _}
import net.spy.memcached.ConnectionFactoryBuilder.{Protocol => SpyProtocol}
import collection.JavaConverters._
import net.spy.memcached.auth.{PlainCallbackHandler, AuthDescriptor}
import scala.Some


class Memcached(config: MemcachedConfiguration) extends Cache {
  def add(key: String, value: Any, exp: Int = 60) =
    if (isRunning)
      instance.add(withPrefix(key), exp, value).get().asInstanceOf[Boolean]
    else
      false

  def fireAdd(key: String, value: Any, exp: Int = 60)(implicit ec: ExecutionContext) {
    if (isRunning)
      instance.add(withPrefix(key), exp, value)
  }

  def asyncAdd(key: String, value: Any, exp: Int)(implicit ec: ExecutionContext): Future[Boolean] = {
    if (isRunning)
      instance.realAsyncAdd(withPrefix(key), value, exp).map(_.isSuccess)
    else
      Future.successful(false)
  }

  def set(key: String, value: Any, exp: Int = 60) =
    if (isRunning)
      instance.set(withPrefix(key), exp, value).get().asInstanceOf[Boolean]
    else
      false

  def fireSet(key: String, value: Any, exp: Int = 60)(implicit ec: ExecutionContext) {
    if (isRunning)
      instance.set(withPrefix(key), exp, value)
  }


  def asyncTransformAndGet[T](key: String, exp: Int)(cb: (Option[T]) => T)(implicit ec: ExecutionContext): Future[T] = {
    def compute(key: String, exp: Int, retry: Int)(cb: (Option[T]) => T)(implicit ec: ExecutionContext): Future[T] =
      instance.realAsyncGets[T](key).flatMap {
        case None =>
          val transformed = cb(None)

          instance.realAsyncAdd(key, transformed, exp).flatMap {
            case status if status.isSuccess =>
              Future.successful(transformed)

            // recursive call
            case status =>
              if (retry < 5)
                compute(key, exp, retry + 1)(cb)
              else
                throw new CacheError("asyncTransformAndGet failed because of too many retries")
          }

        case Some(cas) =>
          val transformed = cb(Some(cas.value))

          instance.realAsyncCAS[T](key, cas.casID, exp, transformed).flatMap {
            case CAS_OK =>
              Future.successful(transformed)
            case CAS_NOT_FOUND =>
              // recursive call
              if (retry < 5)
                compute(key, exp, retry + 1)(cb)
              else
                throw new CacheError("asyncTransformAndGet failed because of too many retries")
            case CAS_EXISTS =>
              // recursive call
              compute(key, exp, retry)(cb)
          }
      }


    compute(key, exp, 0)(cb)
  }

  def asyncCAS[T](key: String, expecting: Option[T], newValue: T, exp: Int = 60)(implicit ec: ExecutionContext): Future[Boolean] =
    expecting match {
      case None =>
        asyncAdd(key, newValue, exp)

      case Some(current) =>
        instance.realAsyncGets[T](key)
          .flatMap {
            case Some(result) if Option(result.value) != expecting =>
              Future.successful(false)
            case Some(result) =>
              instance.realAsyncCAS(key, result.casID, exp, newValue).map {
                case CAS_OK => true
                case CAS_NOT_FOUND => false
                case CAS_EXISTS => false
              }
            case None =>
              Future.successful(false)
          }
    }

  def get[A](key: String): Option[A] =
    if (isRunning)
      Option(instance.get(withPrefix(key))).map(x => x.asInstanceOf[A])
    else
      None

  def getAsync[A](key: String)(implicit ec: ExecutionContext): Future[A] =
    if (isRunning)
      instance.realAsyncGet[A](withPrefix(key))
    else
      Future.failed(CacheClientNotRunning)

  def getAsyncOpt[A](key: String)(implicit ec: ExecutionContext): Future[Option[A]] =
    if (isRunning)
      instance.realAsyncGet[A](withPrefix(key)).map(x => Option(x)).recover {
        case _: NotFoundInCacheError =>
          None
      }
    else
      Future.successful(None)

  def getBulk(keys: Seq[String]): Map[String, Any] = {
    val values: java.util.Map[String, AnyRef] =
      instance.getBulk(keys.map(k => withPrefix(k)).iterator.asJava)

    values.asScala.toMap.asInstanceOf[Map[String, Any]].collect {
      case (k,v) if v != null =>
        (withoutPrefix(k), v)
    }
  }

  def getAsyncBulk(keys: Seq[String])(implicit ec: ExecutionContext): Future[Map[String, Any]] =
    instance.realAsyncGetBulk[Any](keys.map(k => withPrefix(k)).iterator.asJava)
      .map(_.collect {
        case (k,v) if v != null =>
          (withoutPrefix(k), v)
      })

  def shutdown() {
    isRunning = false
    instance.shutdown(500, TimeUnit.SECONDS)
  }

  private[this] def withPrefix(key: String) =
    config.keysPrefix.map(p => p + "-" + key).getOrElse(key)
  private[this] def withoutPrefix(key: String) =
    config.keysPrefix.map(p => if (key.startsWith(p + "-")) key.drop(p.length + 1) else key)
      .getOrElse(key)

  @volatile
  private[this] var isRunning = true
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
        .setOpTimeout(config.operationTimeout.toMillis)

      config.authentication match {
        case Some(credentials) =>
          builder.setAuthDescriptor(
            new AuthDescriptor(Array("PLAIN"),
              new PlainCallbackHandler(credentials.username, credentials.password)))
        case None =>
          builder
      }
    }

    val addresses = AddrUtil.getAddresses(config.addresses)
    new WrappedMemcachedClient(conn.build(), addresses)
  }
}
