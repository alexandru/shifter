package shifter.cache.inmemory

import shifter.cache.{KeyNotInCacheException, Cache}
import concurrent.duration.Duration
import concurrent.{Future, ExecutionContext}
import shifter.concurrency.atomic.Ref
import collection.SortedMap
import annotation.tailrec


/**
 * Simple and dumb implementation of an in-memory cache.
 */
final class InMemoryCache(maxElems: Int = 100) extends Cache {

  def get[T](key: String)(implicit ec: ExecutionContext): Future[Option[T]] = {
    val result = cache.get.get(CacheKey(key)).flatMap {
      case value if !value.key.isExpired() =>
        Some(value.value.asInstanceOf[T])
      case _ =>
        None
    }

    Future.successful(result)
  }

  def getOrElse[T](key: String, default: => T)(implicit ec: ExecutionContext): Future[T] =
    get(key).map(_.getOrElse(default))

  def apply[T](key: String)(implicit ec: ExecutionContext): Future[T] =
    get[T](key).flatMap {
      case Some(value) =>
        Future.successful(value)
      case _ =>
        Future { throw new KeyNotInCacheException("inmemory." + key) }
    }

  def add[T](key: String, value: T, exp: Duration)(implicit ec: ExecutionContext): Future[Boolean] =
    if (value == null)
      Future.successful(false)

    else {
      val updated = cache.transformAndExtract { map =>
        val cacheKey = CacheKey(key)
        val existingValue = map.get(cacheKey).filterNot(_.key.isExpired())

        existingValue match {
          case Some(_) =>
            (map, false)
          case None =>
            val limitedMap = makeRoom(map)
            val newKey = CacheKey(key, exp)
            val newMap = limitedMap.updated(newKey, CacheValue(newKey, value))
            (newMap, true)
        }
      }

      Future.successful(updated)
    }

  def set[T](key: String, value: T, exp: Duration)(implicit ec: ExecutionContext): Future[Unit] = {
    if (value != null)
      cache.transform { map =>
        val cacheKey = CacheKey(key)
        val existingValue = map.get(cacheKey).filterNot(_.key.isExpired())

        existingValue match {
          case Some(v) =>
            val newKey = CacheKey(key, exp)
            map.updated(newKey, CacheValue(newKey, value))
          case None =>
            val limitedMap = makeRoom(map)
            val newKey = CacheKey(key, exp)
            val newMap = limitedMap.updated(newKey, CacheValue(newKey, value))
            newMap
        }
      }

    Future.successful(())
  }


  def delete(key: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val cacheKey = CacheKey(key)
    val oldMap = cache.getAndTransform { map =>
      map - cacheKey
    }
    Future.successful(oldMap.contains(cacheKey))
  }

  def getBulk(keys: Traversable[String])(implicit ec: ExecutionContext): Future[Map[String, Any]] = {
    val map = cache.get
    val currentTS = System.currentTimeMillis()
    val results = keys.map { k =>
      val cacheKey = CacheKey(k)
      map.get(cacheKey).collect {
        case v if !v.key.isExpired(currentTS) =>
          (k, v.value)
      }
    }

    Future.successful(results.flatten.toMap)
  }


  private[this] def compareAndSet[T](key: String, expecting: Option[T], newValue: T, exp: Duration)(implicit ec: ExecutionContext): Boolean = {
    val currentTS = System.currentTimeMillis()

    cache.transformAndExtract { map =>
      val cacheKey = CacheKey(key)

      map.get(cacheKey) match {
        case current @ Some(v) if !v.key.isExpired(currentTS) =>
          if (current.map(_.value) == expecting)  {
            val newKey = CacheKey(key, exp)
            (map.updated(newKey, CacheValue(newKey, newValue)), true)
          }
          else
            (map, false)
        case _ =>
          if (expecting == None) {
            val newKey = CacheKey(key, exp)
            val limited = makeRoom(map)
            (limited.updated(newKey, CacheValue(newKey, newValue)), true)
          }
          else
            (map, false)
      }
    }
  }

  def cas[T](key: String, expecting: Option[T], newValue: T, exp: Duration)(implicit ec: ExecutionContext): Future[Boolean] =
    Future.successful(compareAndSet(key, expecting, newValue, exp))

  @tailrec
  def transformAndGet[T](key: String, exp: Duration)(cb: (Option[T]) => T)(implicit ec: ExecutionContext): Future[T] = {
    val current = cache.get.get(CacheKey(key)).collect { case v if !v.key.isExpired() => v.value.asInstanceOf[T] }
    val update = cb(current)

    if (!compareAndSet(key, current, update, exp))
      transformAndGet(key, exp)(cb)
    else
      Future.successful(update)
  }

  @tailrec
  def getAndTransform[T](key: String, exp: Duration)(cb: (Option[T]) => T)(implicit ec: ExecutionContext): Future[Option[T]] = {
    val current = cache.get.get(CacheKey(key)).collect { case v if !v.key.isExpired() => v.value.asInstanceOf[T] }
    val update = cb(current)

    if (!compareAndSet(key, current, update, exp))
      getAndTransform(key, exp)(cb)
    else
      Future.successful(current)
  }

  def shutdown() {}

  private[this] def makeRoom(map: SortedMap[CacheKey, CacheValue]): SortedMap[CacheKey, CacheValue] = {
    if (map.size >= maxElems) {
      val currentTS = System.currentTimeMillis()
      val notExpired = map.dropWhile(_._1.isExpired(currentTS))
      val withinLimit = if (notExpired.size >= maxElems)
        notExpired.drop(notExpired.size - maxElems + 1)
      else
        notExpired
      withinLimit
    }
    else
      map
  }

  val cache = Ref(SortedMap.empty[CacheKey, CacheValue])
}
