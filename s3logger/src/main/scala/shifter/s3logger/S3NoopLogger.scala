package shifter.s3logger

import support.UploadedFileInfo

class S3NoopLogger private[s3logger] () extends S3Logger {
  def write(content: Array[Byte]) {}

  def rotate(forced: Boolean) =
    Seq.empty[UploadedFileInfo]

  def shutdown() {}
}
