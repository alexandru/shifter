package shifter.s3logger

import support.UploadedFileInfo
import java.nio.ByteBuffer


trait S3Logger {
  def write(str: String, charset: String = "UTF-8")
  def write(b: Array[Byte])
  def write(b: Array[Byte], off: Int, len: Int)
  def write(b: ByteBuffer)

  def rotate(forced: Boolean): Seq[UploadedFileInfo]
}

object S3Logger {
  val noop = new S3Logger {
    def rotate(forced: Boolean): Seq[UploadedFileInfo] = Seq.empty
    def write(str: String, charset: String) {}
    def write(b: Array[Byte]) {}
    def write(b: Array[Byte], off: Int, len: Int) {}
    def write(b: ByteBuffer) {}
  }
}