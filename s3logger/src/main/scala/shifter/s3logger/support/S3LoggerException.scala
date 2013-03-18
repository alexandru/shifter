package shifter.s3logger.support

case class S3LoggerException(msg: String)
  extends RuntimeException(msg)