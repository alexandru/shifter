package shifter.cache

import concurrent.duration._

case class MemcachedConfiguration(
  addresses: String,
  authentication: Option[MemcachedAuthConfiguration] = None,
  keysPrefix: Option[String] = None,
  protocol: Protocol.Type = Protocol.Binary,
  failureMode: FailureMode.Value = FailureMode.Retry,
  operationTimeout: FiniteDuration = 1.second
)

object Protocol extends Enumeration {
  type Type = Value
  val Binary, Text = Value
}

object FailureMode extends Enumeration {
  val Retry, Cancel, Redistribute = Value
}

case class MemcachedAuthConfiguration(
  username: String,
  password: String
)