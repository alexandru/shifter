package shifter.s3logger

class S3NoopLogger private[s3logger] () extends S3Logger {
  def write(content: Array[Byte]) {}

  def rotate(forced: Boolean) {}

  def shutdown() {}
}
