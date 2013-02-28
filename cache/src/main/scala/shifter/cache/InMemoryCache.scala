package shifter.cache

import concurrent.Future
import concurrent.stm._
import errors.NotFoundInCacheError
import scala.concurrent.ExecutionContext.Implicits.global


/**
 * Simple and dumb implementation of an in-memory cache.
 */
class InMemoryCache extends Cache {
  private[this] val cache = Ref(Vector.empty[Element])
  private[this] val MaxElements = 200
  private[this] def currentTime = System.currentTimeMillis() / 1000

  private[this] case class Element(key: String, value: Any, expiresTS: Long) {
    def isExpired =
      (System.currentTimeMillis() / 1000) >= expiresTS
  }

  private[this] def cleanup(list: Vector[Element]) =
    if (list.length < MaxElements)
      list
    else
      list.sortWith((a,b) => a.expiresTS >= b.expiresTS).take(MaxElements).filterNot(_.isExpired)

  def add(key: String, value: Any, exp: Int): Boolean = atomic { implicit txn =>
    if (cache().exists(elem => elem.key == key && !elem.isExpired))
      false
    else {
      cache() = cleanup(cache()) :+ Element(key, value, currentTime + exp)
      true
    }
  }

  def fireAdd(key: String, value: Any, exp: Int) {
    Future {
      atomic { implicit txn =>
        if (!cache().exists(elem => elem.key == key && !elem.isExpired))
          cache() = cleanup(cache() :+ Element(key, value, currentTime + exp))
      }
    }
  }

  def set(key: String, value: Any, exp: Int): Boolean = {
    cache.single.transform { list =>
      cleanup(list).filterNot(_.key == key) :+ Element(key, value, currentTime + exp)
    }
    true
  }

  def fireSet(key: String, value: Any, exp: Int) {
    Future {
      cache.single.transform { list =>
        cleanup(list).filterNot(_.key == key) :+ Element(key, value, currentTime + exp)
      }
    }
  }

  def get[A](key: String): Option[A] =
    cache.single.get.find(x => x.key == key && !x.isExpired).map(_.value.asInstanceOf[A])

  def getAsync[A](key: String): Future[A] =
    get[A](key).map(x => Future.successful(x))
      .getOrElse(Future.failed(new NotFoundInCacheError(key)))

  def getAsyncOpt[A](key: String): Future[Option[A]] =
    Future.successful(get[A](key))

  def getBulk(keys: Seq[String]): Map[String, Any] =
    cache.single.get.collect {
      case elem if keys.contains(elem.key) && !elem.isExpired =>
        (elem.key, elem.value)
    }
      .toMap

  def getAsyncBulk(keys: Seq[String]): Future[Map[String, Any]] =
    Future.successful(getBulk(keys))

  def shutdown() {
    cache.single.transform(x => Vector.empty)
  }
}