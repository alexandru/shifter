package shifter.s3logger


case class S3LoggerException(msg: String)
  extends RuntimeException(msg)