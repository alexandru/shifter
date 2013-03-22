package shifter.s3logger

import support.{UploadedFileInfo, S3LoggerException}

trait S3Logger {
  def write(content: Array[Byte])
  def rotate(forced: Boolean): Seq[UploadedFileInfo]
}

object S3Logger {
  def apply(config: Configuration) = {
    if (config.isEnabled && !config.aws.isDefined)
      throw S3LoggerException("S3Logger is enabled, but AWS credentials are not defined")
    if (config.isEnabled)
      new S3SynchronizedLogger(config)
    else
      new S3NoopLogger()
  }
}