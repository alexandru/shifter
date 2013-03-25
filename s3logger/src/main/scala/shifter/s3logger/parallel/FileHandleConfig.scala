package shifter.s3logger.parallel

case class FileHandleConfig(
  localDirectory: String,
  filePrefix: String,
  fileSuffix: String
)