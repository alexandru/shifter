package shifter.s3logger

import support.UploadedFileInfo


trait S3Logger {
  def write(content: String)
  def rotate(forced: Boolean): Seq[UploadedFileInfo]
}

object S3Logger {
  val noop = new S3Logger {
    def rotate(forced: Boolean): Seq[UploadedFileInfo] = Seq.empty
    def write(content: String) {}
  }
}