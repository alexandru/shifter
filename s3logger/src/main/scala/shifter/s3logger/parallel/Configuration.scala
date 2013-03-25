package shifter.s3logger.parallel

import concurrent.duration._

case class Configuration(
  collection: String,
  localDirectory: String,
  expiry: FiniteDuration = 30.minutes,
  maxSizeMB: Int = 1000,
  aws: AWSConfiguration,
  parallelism: Int
)

case class AWSConfiguration(
  accessKey: String,
  secretKey: String,
  bucketName: String
)