package shifter.web.api.requests

import shifter.web.api.base.{HttpMethod, Cookie}

case class FormRequest(
  method: HttpMethod.Value,
  path: String,
  domain: String,
  port: Int,
  protocol: String,
  url: String,
  query: Option[String],
  headers: Map[String, Seq[String]],
  remoteAddress: String,
  cookies: Map[String, Cookie],
  body: Map[String, Seq[String]]
)
extends HttpRequest[Map[String, Seq[String]]] {
  def param(key: String): Option[String] =
    paramsSimple.get(key)

  def paramList(key: String): Seq[String] =
    body.get(key).getOrElse(Seq.empty)

  lazy val paramsSimple: Map[String, String] =
    body.flatMap {
      case (key, list) =>
        list.headOption.map(value => (key, value))
    }
}


