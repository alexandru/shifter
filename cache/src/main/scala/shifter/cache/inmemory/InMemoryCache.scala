package shifter.cache.inmemory

import shifter.cache.{CacheException, Cache}
import concurrent.duration.Duration
import concurrent.{Future, ExecutionContext}
import shifter.concurrency.utils.Ref
import annotation.tailrec


/**
 * Simple and dumb implementation of an in-memory cache.
 */
final class InMemoryCache(maxElems: Int = 100) extends Cache {
  override def get[T](key: String)(implicit ec: ExecutionContext): Option[T] =
    cacheRef.get.get(key).flatMap {
      case value if !isExpired(value) =>
        Some(value.value.asInstanceOf[T])
      case _ =>
        None
    }

  def asyncGet[T](key: String)(implicit ec: ExecutionContext): Future[Option[T]] =
    Future.successful(get(key))


  override def add[T](key: String, value: T, exp: Duration)(implicit ec: ExecutionContext): Boolean =
    if (value == null)
      false

    else
      cacheRef.transformAndExtract { map =>
        val existingValue = map.get(key).filterNot(v => isExpired(v))

        existingValue match {
          case Some(_) =>
            (map, false)
          case None =>
            val newMap = makeRoom(map).updated(key, CacheValue(value, exp))
            (newMap, true)
        }
      }

  def asyncAdd[T](key: String, value: T, exp: Duration)(implicit ec: ExecutionContext): Future[Boolean] =
    Future.successful(add(key, value, exp))


  override def set[T](key: String, value: T, exp: Duration)(implicit ec: ExecutionContext) {
    if (value != null)
      cacheRef.transform { map =>
        val existingValue = map.get(key).filterNot(v => isExpired(v))

        existingValue match {
          case Some(v) =>
            map.updated(key, CacheValue(value, exp))
          case None =>
            val limitedMap = makeRoom(map)
            val newMap = limitedMap.updated(key, CacheValue(value, exp))
            newMap
        }
      }
  }

  def asyncSet[T](key: String, value: T, exp: Duration)(implicit ec: ExecutionContext): Future[Unit] = {
    set(key, value, exp)
    Future.successful(())
  }


  override def delete(key: String)(implicit ec: ExecutionContext): Boolean = {
    val oldMap = cacheRef.getAndTransform { map =>
      map - key
    }
    oldMap.contains(key) && !isExpired(oldMap(key))
  }

  def asyncDelete(key: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    Future.successful(delete(key))
  }

  override def bulk(keys: Traversable[String])(implicit ec: ExecutionContext): Map[String, Any] = {
    val map = cacheRef.get
    val currentTS = System.currentTimeMillis()
    val results = keys.map { k =>
      map.get(k).collect {
        case v if !isExpired(v, currentTS) =>
          (k, v.value)
      }
    }

    results.flatten.toMap
  }

  def asyncBulk(keys: Traversable[String])(implicit ec: ExecutionContext): Future[Map[String, Any]] =
    Future.successful(bulk(keys))

  private[this] def compareAndSet[T](key: String, expecting: Option[T], newValue: T, exp: Duration)(implicit ec: ExecutionContext): Boolean = {
    val currentTS = System.currentTimeMillis()

    cacheRef.transformAndExtract { map =>
      map.get(key) match {
        case current @ Some(v) if !isExpired(v, currentTS) =>
          if (current.map(_.value) == expecting)  {
            (map.updated(key, CacheValue(newValue, exp)), true)
          }
          else
            (map, false)
        case _ =>
          if (expecting == None) {
            val limited = makeRoom(map)
            (limited.updated(key, CacheValue(newValue, exp)), true)
          }
          else
            (map, false)
      }
    }
  }

  def cas[T](key: String, expecting: Option[T], newValue: T, exp: Duration)(implicit ec: ExecutionContext): Future[Boolean] =
    Future.successful(compareAndSet(key, expecting, newValue, exp))

  def transformAndGet[T](key: String, exp: Duration)(cb: (Option[T]) => T)(implicit ec: ExecutionContext): Future[T] = {
    @tailrec
    def loop: T = {
      val current = cacheRef.get.get(key).collect { case v if !isExpired(v) => v.value.asInstanceOf[T] }
      val update = cb(current)

      if (!compareAndSet(key, current, update, exp))
        loop
      else
        update
    }

    Future(loop)
  }

  def getAndTransform[T](key: String, exp: Duration)(cb: (Option[T]) => T)(implicit ec: ExecutionContext): Future[Option[T]] = {
    @tailrec
    def loop: Option[T] = {
      val current = cacheRef.get.get(key).collect { case v if !isExpired(v) => v.value.asInstanceOf[T] }
      val update = cb(current)

      if (!compareAndSet(key, current, update, exp))
        loop
      else
        current
    }

    Future(loop)
  }

  def shutdown() {
    cacheRef.set(Map.empty)
  }

  private[this] def makeRoom(map: Map[String, CacheValue]): Map[String, CacheValue] = {
    val currentTS = System.currentTimeMillis()
    val newMap = if (map.size >= maxElems)
      map.filterNot { case (key, value) => isExpired(value, currentTS) }
    else
      map

    if (newMap.size < maxElems)
      newMap
    else
      throw new CacheException("InMemoryCache cannot hold any more elements")
  }

  private[this] def isExpired(value: CacheValue, currentTS: Long = System.currentTimeMillis()) =
    value.expiresTS <= currentTS

  private[this] val cacheRef = Ref(Map.empty[String, CacheValue])
}
