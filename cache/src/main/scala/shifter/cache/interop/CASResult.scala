package shifter.cache.interop

sealed trait CASResult

case object CAS_OK extends CASResult
case object CAS_NOT_FOUND extends CASResult
case object CAS_EXISTS extends CASResult