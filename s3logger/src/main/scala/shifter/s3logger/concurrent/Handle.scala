package shifter.s3logger.concurrent

import java.io.{Writer, FileOutputStream, File}

case class Handle(
  file: File,
  fileStream: FileOutputStream,
  out: Writer,
  createdTS: Long
)
