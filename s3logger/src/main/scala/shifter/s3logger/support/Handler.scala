package shifter.s3logger.support

import java.io.{OutputStream, FileOutputStream, File}

case class Handler(
  file: File,
  fileStream: FileOutputStream,
  out: OutputStream,
  createdTS: Long
)
