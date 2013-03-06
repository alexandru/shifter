package shifter.web.api

import java.io.InputStream

case class FileInfo(
  name: String,
  inputStream: InputStream,
  contentType: String,
  size: Long
)
