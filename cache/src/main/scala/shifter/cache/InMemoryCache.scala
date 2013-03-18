package shifter.cache

import concurrent.duration.{FiniteDuration, Duration}
import concurrent.{Future, ExecutionContext}
import util.Random
import java.util.concurrent.atomic.AtomicLong
import concurrent.stm._

/**
 * Simple and dumb implementation of an in-memory cache.
 *
 * Can store a maximum amount of elements as specified in the constructor.
 * When that limit is reached, the cleanup process is executed.
 *
 * On cleanup the expired elements are filtered out. If there are more than
 * maxElems that are still fresh, then it drops elements randomly.
 *
 * It is thread-safe and uses Scala-STM / optimistic locking for maintaining the
 * interval dictionary of keys / values.
 *
 * See the parent trait (Cache) for API docs.
 */
class InMemoryCache(maxElems: Int = 5000) extends Cache {
  private[this] def millis(exp: Duration) = exp match {
    case finite: FiniteDuration =>
      finite.toMillis
    case _ =>
      1000L * 60 * 60 * 24 * 365
  }

  def add[T](key: String, value: T, exp: Duration = defaultExpiry)(implicit ec: ExecutionContext): Future[Boolean] =
    Future {
      val previous = cache.single.getAndTransform { map =>
        if (!map.contains(key) || isExpired(map(key)))
          map.updated(key, Elem(
            value = value,
            expiresTS = System.currentTimeMillis() + millis(exp)
          ))
        else
          map
      }

      val isAdded = !previous.contains(key) || isExpired(previous(key))
      val count = previous.size + (if (isAdded) 1 else 0)
      cleanup(count)

      isAdded
    }

  def set[T](key: String, value: T, exp: Duration = defaultExpiry)(implicit ec: ExecutionContext): Future[Unit] =
    Future {
      val map = cache.single.transformAndGet { map => map.updated(key, Elem(
        value = value,
        expiresTS = System.currentTimeMillis() + millis(exp)
      ))}

      cleanup(map.size)
    }

  def delete(key: String)(implicit ec: ExecutionContext): Future[Boolean] =
    Future {
      val map = cache.single.getAndTransform(_ - key)
      cleanup(map.size)
      map.contains(key)
    }

  def apply[T](key: String)(implicit ec: ExecutionContext): Future[T] =
    cache.single.get.get(key) match {
      case Some(elem) if !isExpired(elem) =>
        Future.successful(elem.value.asInstanceOf[T])
      case _ =>
        Future { throw new KeyNotInCacheException("inmemory." + key) }
    }

  def get[T](key: String)(implicit ec: ExecutionContext): Future[Option[T]] =
    cache.single.get.get(key) match {
      case Some(elem) if !isExpired(elem) =>
        Future.successful(Some(elem.value.asInstanceOf[T]))
      case _ =>
        Future.successful(None)
    }

  def getOrElse[T](key: String, default: => T)(implicit ec: ExecutionContext): Future[T] =
    cache.single.get.get(key) match {
      case Some(elem) if !isExpired(elem) =>
        Future.successful(elem.value.asInstanceOf[T])
      case _ =>
        Future.successful(default)
    }

  def getBulk(keys: Traversable[String])(implicit ec: ExecutionContext): Future[Map[String, Any]] = {
    val keysSet = keys.toSet
    val result = cache.single.get.collect {
      case (key, value) if keysSet(key) && !isExpired(value) =>
        (key, value.value)
    }

    Future.successful(result)
  }

  def cas[T](key: String, expecting: Option[T], newValue: T, exp: Duration)(implicit ec: ExecutionContext): Future[Boolean] =
    Future {
      def hasElement(map: Map[String, Elem]) =
        map.get(key).filterNot(isExpired).map(_.value.asInstanceOf[T]) == expecting

      val previous = cache.single.getAndTransform { map =>
        if (hasElement(map))
          map.updated(key, Elem(
            value = newValue,
            expiresTS = System.currentTimeMillis() + millis(exp)
          ))
        else
          map
      }

      cleanup(previous.size)
      hasElement(previous)
    }

  def transformAndGet[T](key: String, exp: Duration)(cb: (Option[T]) => T)(implicit ec: ExecutionContext): Future[T] =
    Future {
      val next = cache.single.transformAndGet { map =>
        val expecting = map.get(key).filterNot(isExpired).map(_.value.asInstanceOf[T])
        val result = cb(expecting)

        map.updated(key, Elem(
          value = result,
          expiresTS = System.currentTimeMillis() + millis(exp)
        ))
      }

      cleanup(next.size)
      next(key).value.asInstanceOf[T]
    }

  def getAndTransform[T](key: String, exp: Duration)(cb: (Option[T]) => T)(implicit ec: ExecutionContext): Future[Option[T]] =
    Future {
      val prev = cache.single.getAndTransform { map =>
        val expecting = map.get(key).filterNot(isExpired).map(_.value.asInstanceOf[T])
        val result = cb(expecting)

        map.updated(key, Elem(
          value = result,
          expiresTS = System.currentTimeMillis() + millis(exp)
        ))
      }

      cleanup(prev.size + 1)
      prev.get(key).map(_.value.asInstanceOf[T])
    }


  def shutdown() {
    cache.single.transform(current => Map.empty)
  }

  private[this] def cleanup(size: Int) {
    if (size > maxElems || System.currentTimeMillis() - lastCleanup.get() > 10000) {
      lastCleanup.set(System.currentTimeMillis())

      cache.single.transform {
        map =>
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
