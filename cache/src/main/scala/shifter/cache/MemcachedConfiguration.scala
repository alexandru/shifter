package shifter.cache

case class MemcachedConfiguration(
  addresses: String,
  authentication: Option[MemcachedAuthConfiguration] = None,
  keysPrefix: Option[String] = None,
  protocol: Protocol.Type = Protocol.Binary
)

object Protocol extends Enumeration {
  type Type = Value
  val Binary, Text = Value
}

case class MemcachedAuthConfiguration(
  username: String,
  password: String
)