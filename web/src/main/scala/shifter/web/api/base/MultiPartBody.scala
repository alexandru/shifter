package shifter.web.api.base

case class MultiPartBody(
  params: Map[String, Seq[String]],
  files: Map[String, FileInfo]
)
