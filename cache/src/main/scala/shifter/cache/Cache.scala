package shifter.cache

import concurrent._
import duration._
import inmemory.InMemoryCache
import memcached.{Memcached, Configuration}

trait Cache {
  protected def defaultExpiry = 10.minutes

  /**
   * Adds a value for a given key, if the key doesn't already exist in the cache store.
   *
   * If the key already exists in the cache, the future returned result will be false and the
   * current value will not be overridden. If the key isn't there already, the value
   * will be set and the future returned result will be true.
   *
   * The expiry time can be Duration.Inf (infinite duration).
   *
   * @return either true, in case the value was set, or false otherwise
   */
  def add[T](key: String, value: T, exp: Duration = defaultExpiry)(implicit ec: ExecutionContext): Future[Boolean]

  /**
   * Sets a (key, value) in the cache store.
   *
   * The expiry time can be Duration.Inf (infinite duration).
   */
  def set[T](key: String, value: T, exp: Duration = defaultExpiry)(implicit ec: ExecutionContext): Future[Unit]

  /**
   * Deletes a key from the cache store.
   *
   * @return true if a key was deleted or false if there was nothing there to delete
   */
  def delete(key: String)(implicit ec: ExecutionContext): Future[Boolean]

  /**
   * Fetches a value from the cache store.
   *
   * @throws KeyNotInCacheException
   */
  def apply[T](key: String)(implicit ec: ExecutionContext): Future[T]

  /**
   * Fetches a value from the cache store.
   *
   * @return Some(value) in case the key is available, or None otherwise (doesn't throw exception on key missing)
   */
  def get[T](key: String)(implicit ec: ExecutionContext): Future[Option[T]]

  /**
   * Fetches a value from the cache store, or a default in case the key is missing.
   */
  def getOrElse[T](key: String, default: => T)(implicit ec: ExecutionContext): Future[T]

  /**
   * Fetches several keys from the cache store at once, returning a map.
   */
  def getBulk(keys: Traversable[String])(implicit ec: ExecutionContext): Future[Map[String, Any]]

  /**
   * Compare and set.
   *
   * @param expecting should be None in case the key is not expected, or Some(value) otherwise
   * @param exp can be Duration.Inf (infinite) for not setting an expiration
   * @return either true (in case the compare-and-set succeeded) or false otherwise
   */
  def cas[T](key: String, expecting: Option[T], newValue: T, exp: Duration = defaultExpiry)(implicit ec: ExecutionContext): Future[Boolean]

  /**
   * Transforms the given key and returns the new value.
   *
   * The cb callback receives the current value
   * (None in case the key is missing or Some(value) otherwise)
   * and should return the new value to store.
   *
   * The method retries until the compare-and-set operation succeeds, so
   * the callback should have no side-effects.
   *
   * This function can be used for atomic incrementers and stuff like that.
   *
   * @return the new value
   */
  def transformAndGet[T](key: String, exp: Duration = defaultExpiry)(cb: Option[T] => T)(implicit ec: ExecutionContext): Future[T]

  /**
   * Transforms the given key and returns the old value as an Option[T]
   * (None in case the key wasn't in the cache or Some(value) otherwise).
   *
   * The cb callback receives the current value
   * (None in case the key is missing or Some(value) otherwise)
   * and should return the new value to store.
   *
   * The method retries until the compare-and-set operation succeeds, so
   * the callback should have no side-effects.
   *
   * This function can be used for atomic incrementers and stuff like that.
   *
   * @return the old value
   */
  def getAndTransform[T](key: String, exp: Duration = defaultExpiry)(cb: Option[T] => T)(implicit ec: ExecutionContext): Future[Option[T]]

  /**
   * Shuts down the cache instance, performs any additional cleanups necessary.
   */
  def shutdown()
}

object Cache {
  def apply(config: Configuration)(implicit ec: ExecutionContext): Cache =
    Memcached(config)

  def inMemory(maxElements: Int)(implicit ec: ExecutionContext): Cache =
    new InMemoryCache(maxElements)
}

