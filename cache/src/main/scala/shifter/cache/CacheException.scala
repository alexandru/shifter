package shifter.cache

/**
 * Super-class for errors thrown in case specific cache-related
 * errors occur.
 */
class CacheException(msg: String) extends RuntimeException(msg)

/**
 * Thrown in case a cache store related operation times out.
 */
class TimeoutException extends CacheException("timed out")

/**
 * Thrown in case a cache store related operation is cancelled
 * (like due to closed / broken connections)
 */
class CancelledException extends CacheException("cancelled")

/**
 * Gets thrown in case the implementation is wrong and
 * misshandled a status. Should never get thrown and
 * if it does, then it's a bug.
 */
class UnhandledStatusException(msg: String) extends CacheException(msg)

/**
 * Gets thrown in case a key is not found in the cache store on #apply().
 */
class KeyNotInCacheException(key: String) extends CacheException(key)