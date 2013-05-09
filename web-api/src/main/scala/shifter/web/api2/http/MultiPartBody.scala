package shifter.web.api2.http

case class MultiPartBody(
  params: Map[String, Seq[String]],
  files: Map[String, FileInfo]
)
