package shifter.web.api.requests

import spray.json._
import shifter.web.api.base.{HttpMethod, Cookie}

case class JsonRequest(
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
  body: JsValue
)
extends HttpRequest[JsValue]


