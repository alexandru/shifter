package shifter.web.api

case class MultiPartBody(
  params: Map[String, Seq[String]],
  files: Map[String, FileInfo]
)
