package shifter.web.api.http

case class MultiPartBody(
  params: Map[String, Seq[String]],
  files: Map[String, FileInfo]
)
