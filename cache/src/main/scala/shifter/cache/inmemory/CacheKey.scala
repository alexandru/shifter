package shifter.cache.inmemory

import concurrent.duration._
import concurrent.duration.{FiniteDuration, Duration}

final class CacheKey(val key: String, val expiresTS: Long = 0) {
  def isExpired(currentTS: Long = System.currentTimeMillis()) =
    expiresTS <= currentTS

  override def equals(obj: Any): Boolean =
    obj match {
      case other: CacheKey if other != null =>
        other.key == this.key
      case _ =>
        false
    }

  override def hashCode(): Int =
    key.hashCode


  override def toString: String =
    "CacheKey(%s, exp=%d)".format(key, expiresTS)
}

object CacheKey {
  def apply(key: String): CacheKey = new CacheKey(key)
  def apply(key: String, expiresTS: Long): CacheKey = new CacheKey(key, expiresTS)
  def apply(key: String, expires: Duration): CacheKey = {
    val expiresTS = expires match {
      case finite: FiniteDuration =>
        System.currentTimeMillis() + finite.toMillis
      case infinite =>
        System.currentTimeMillis() + 365.days.toMillis
    }
    CacheKey(key, expiresTS)
  }

  implicit object CacheKeyOrdering extends Ordering[CacheKey] {
    val stringOrdering = implicitly[Ordering[String]]

    def compare(x: CacheKey, y: CacheKey): Int = {
      val currentTS = System.currentTimeMillis()

      if (x.key == y.key)
        0
      else if (x.isExpired(currentTS) && y.isExpired(currentTS))
        0
      else if (x.isExpired(currentTS))
        -1
      else if (y.isExpired(currentTS))
        1
      else if (x.expiresTS < y.expiresTS)
        -1
      else if (x.expiresTS > y.expiresTS)
        1
      else
        -1
    }
  }
}
