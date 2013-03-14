package shifter.s3logger

import concurrent.duration._

case class Configuration(
  collection: String,
  localDirectory: String,
  expiry: FiniteDuration = 30.minutes,
  maxSizeMB: Int = 1000,
  aws: Option[AWSConfiguration] = None,
  isEnabled: Boolean = false,
  syncMillis: Int = 500
)

case class AWSConfiguration(
  accessKey: String,
  secretKey: String,
  bucketName: String
)