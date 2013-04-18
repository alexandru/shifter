package shifter.web.api.http

import java.io.InputStream

case class FileInfo(
  name: String,
  inputStream: InputStream,
  contentType: String,
  size: Long
)
