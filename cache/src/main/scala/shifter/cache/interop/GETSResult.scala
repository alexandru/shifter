package shifter.cache.interop

case class GETSResult[T](
  value: T,
  casID: Long
)