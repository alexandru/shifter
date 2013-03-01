package shifter.cache

import concurrent.Future
import errors.{NotFoundInCacheError, CacheClientNotRunning}
import java.util.concurrent.TimeUnit
import net.spy.memcached.{WrappedMemcachedClient, AddrUtil, FailureMode, ConnectionFactoryBuilder}
import net.spy.memcached.ConnectionFactoryBuilder.{Protocol => SpyProtocol}
import scala.concurrent.ExecutionContext.Implicits.global
import collection.JavaConverters._
import net.spy.memcached.auth.{PlainCallbackHandler, AuthDescriptor}

class Memcached(config: MemcachedConfiguration) extends Cache {
  def add(key: String, value: Any, exp: Int = 60) =
    if (isRunning)
      instance.add(withPrefix(key), exp, value).get().asInstanceOf[Boolean]
    else
      false

  def fireAdd(key: String, value: Any, exp: Int = 60) {
    if (isRunning)
      instance.add(withPrefix(key), exp, value)
  }

  def set(key: String, value: Any, exp: Int = 60) =
    if (isRunning)
      instance.set(withPrefix(key), exp, value).get().asInstanceOf[Boolean]
    else
      false

  def fireSet(key: String, value: Any, exp: Int = 60) {
    if (isRunning)
      instance.set(withPrefix(key), exp, value)
  }

  def get[A](key: String): Option[A] =
    if (isRunning)
      Option(instance.get(withPrefix(key))).map(x => x.asInstanceOf[A])
    else
      None

  def getAsync[A](key: String): Future[A] =
    if (isRunning)
      instance.realAsyncGet[A](withPrefix(key))
    else
      Future.failed(CacheClientNotRunning)

  def getAsyncOpt[A](key: String): Future[Option[A]] =
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

  def getAsyncBulk(keys: Seq[String]): Future[Map[String, Any]] =
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
        .setFailureMode(FailureMode.Retry)

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
