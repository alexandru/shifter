package shifter.s3logger.async

import concurrent.duration._

case class Configuration(
  collection: String,
  localDirectory: String,
  expiry: FiniteDuration = 30.minutes,
  maxSizeMB: Int = 1000,
  aws: AWSConfiguration
)

case class AWSConfiguration(
  accessKey: String,
  secretKey: String,
  bucketName: String
)