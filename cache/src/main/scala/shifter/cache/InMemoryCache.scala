package shifter.cache

import concurrent.{ExecutionContext, Future}
import concurrent.stm._
import errors.NotFoundInCacheError
import java.util.concurrent.atomic.AtomicLong
import util.Random


/**
 * Simple and dumb implementation of an in-memory cache.
 *
 * Can store a maximum amount of elements as specified in the constructor.
 * When that limit is reached, the cleanup process is executed.
 *
 * On cleanup the expired elements are filtered out. If there are more than
 * maxElems that are still fresh, then it drops elements randomly.
 */
class InMemoryCache(maxElems: Int = 5000) extends Cache {
  def add(key: String, value: Any, exp: Int): Boolean = {
    val previous = cache.single.getAndTransform { map =>
      if (!map.contains(key) || isExpired(map(key)))
        map.updated(key, Elem(
          value = value,
          expiresTS = System.currentTimeMillis() + exp * 1000
        ))
      else
        map
    }

    val isAdded = !previous.contains(key) || isExpired(previous(key))
    val count = previous.size + (if (isAdded) 1 else 0)
    cleanup(count)
    isAdded
  }

  def fireAdd(key: String, value: Any, exp: Int)(implicit ec: ExecutionContext) {
    Future(add(key, value, exp))
  }

  def set(key: String, value: Any, exp: Int): Boolean = {
    val map = cache.single.transformAndGet { map => map.updated(key, Elem(
      value = value,
      expiresTS = System.currentTimeMillis() + exp * 1000
    ))}

    cleanup(map.size)
    true
  }

  def fireSet(key: String, value: Any, exp: Int)(implicit ec: ExecutionContext) {
    Future(set(key, value, exp))
  }

  def get[A](key: String): Option[A] = {
    cache.single.get.get(key) match {
      case Some(elem) if !isExpired(elem) =>
        Option(elem.value).asInstanceOf[Option[A]]
      case _ =>
        None
    }
  }

  def getAsync[A](key: String)(implicit ec: ExecutionContext): Future[A] =
    cache.single.get.get(key) match {
      case Some(elem) if !isExpired(elem) && elem.value != null =>
        Future.successful(elem.value.asInstanceOf[A])
      case _ =>
        Future.failed(new NotFoundInCacheError(key))
    }

  def getAsyncOpt[A](key: String)(implicit ec: ExecutionContext): Future[Option[A]] =
    cache.single.get.get(key) match {
      case Some(elem) if !isExpired(elem) =>
        Future.successful(Option(elem.value).asInstanceOf[Option[A]])
      case _ =>
        Future.successful(None)
    }

  def getBulk(keys: Seq[String]): Map[String, Any] = {
    val keysSet = keys.toSet
    cache.single.get.collect {
      case (key, value) if keysSet(key) && !isExpired(value) =>
        (key, value.value)
    }
  }

  def getAsyncBulk(keys: Seq[String])(implicit ec: ExecutionContext): Future[Map[String, Any]] =
    Future.successful(getBulk(keys))

  def shutdown() {
    cache.single.set(Map.empty)
  }

  private[this] def cleanup(size: Int) {
    if (size > maxElems || System.currentTimeMillis() - lastCleanup.get() > 10000) {
      lastCleanup.set(System.currentTimeMillis())

      cache.single.transform { map =>
        val filtered = map.filter(elem => !isExpired(elem._2))
        Random.shuffle(filtered).take(maxElems).toMap
      }
    }
  }

  private[this] def isExpired(elem: Elem) = {
    val current = System.currentTimeMillis()
    current > elem.expiresTS
  }

  private[this] case class Elem(
    value: Any,
    expiresTS: Long
  )

  private[this] val lastCleanup = new AtomicLong(System.currentTimeMillis())
  private[this] val cache = Ref(Map.empty[String, Elem])
}