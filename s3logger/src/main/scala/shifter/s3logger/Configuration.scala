package shifter.s3logger

import concurrent.duration._

case class Configuration(
  collection: String,
  localDirectory: String,
  interval: FiniteDuration = 30.minutes,
  initialDelay: FiniteDuration = 10.seconds,
  maxSizeMB: Int = 1000,
  aws: Option[AWSConfiguration] = None,
  isEnabled: Boolean = false,
  rotateInBackground: Boolean = false
)

case class AWSConfiguration(
  accessKey: String,
  secretKey: String,
  bucketName: String
)