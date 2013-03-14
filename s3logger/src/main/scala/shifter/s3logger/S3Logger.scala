package shifter.s3logger

trait S3Logger {
  def write(content: Array[Byte])
  def rotate(forced: Boolean): Boolean

  def write(line: String) {
    if (line.endsWith("\n"))
      write(line.getBytes("UTF-8"))
    else
      write((line + "\n").getBytes("UTF-8"))
  }
}

object S3Logger {
  def apply(config: Configuration) = {
    if (config.isEnabled && !config.aws.isDefined)
      throw S3LoggerException("S3Logger is enabled, but AWS credentials are not defined")
    if (config.isEnabled)
      new S3ActiveLogger(config)
    else
      new S3NoopLogger()
  }
}