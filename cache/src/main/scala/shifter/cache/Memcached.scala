package shifter.cache

import concurrent.Future
import errors.{NotFoundInCacheError, CacheClientNotRunning}
import java.util.concurrent.TimeUnit
import net.spy.memcached.{WrappedMemcachedClient, AddrUtil, FailureMode, ConnectionFactoryBuilder}
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import scala.concurrent.ExecutionContext.Implicits.global
import collection.JavaConverters._

/**
 * Started by: Alexandru Nedelcu
 * Copyright @2013 Epigrams, Inc.
 */
class Memcached(addresses: String) extends Cache {
  def add(key: String, value: Any, exp: Int = 60) =
    if (isRunning)
      instance.add(key, exp, value).get().asInstanceOf[Boolean]
    else
      false

  def fireAdd(key: String, value: Any, exp: Int = 60) {
    if (isRunning)
      instance.add(key, exp, value)
  }

  def set(key: String, value: Any, exp: Int = 60) =
    if (isRunning)
      instance.set(key, exp, value).get().asInstanceOf[Boolean]
    else
      false

  def fireSet(key: String, value: Any, exp: Int = 60) {
    if (isRunning)
      instance.set(key, exp, value)
  }

  def get[A](key: String): Option[A] =
    if (isRunning)
      Option(instance.get(key)).map(x => x.asInstanceOf[A])
    else
      None

  def getAsync[A](key: String): Future[A] =
    if (isRunning)
      instance.realAsyncGet[A](key)
    else
      Future.failed(CacheClientNotRunning)

  def getAsyncOpt[A](key: String): Future[Option[A]] =
    if (isRunning)
      instance.realAsyncGet[A](key).map(x => Option(x)).recover {
        case _: NotFoundInCacheError =>
          None
      }
    else
      Future.successful(None)

  def getBulk(keys: Seq[String]): Map[String, Any] = {
    val values: java.util.Map[String, AnyRef] = instance.getBulk(keys.iterator.asJava)
    values.asScala.toMap.asInstanceOf[Map[String, Any]].filter {
      case (k,v) => v != null
    }
  }

  def getAsyncBulk(keys: Seq[String]): Future[Map[String, Any]] =
    instance.realAsyncGetBulk[Any](keys.iterator.asJava)

  def shutdown() {
    isRunning = false
    instance.shutdown(500, TimeUnit.SECONDS)
  }

  @volatile
  private[this] var isRunning = true
  private[this] val instance = {
    val conn = new ConnectionFactoryBuilder()
      .setProtocol(Protocol.BINARY)
      .setDaemon(true)
      .setFailureMode(FailureMode.Retry).build()

    val addrs = AddrUtil.getAddresses(addresses)
    new WrappedMemcachedClient(conn, addrs)
  }
}