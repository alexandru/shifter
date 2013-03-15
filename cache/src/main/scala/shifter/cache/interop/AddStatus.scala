package shifter.cache.interop

case class AddStatus(
  isSuccess: Boolean,
  casID: Option[Long]
)
