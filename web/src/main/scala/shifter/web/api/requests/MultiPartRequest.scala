package shifter.web.api.requests

import shifter.web.api.http.{MultiPartBody, HttpMethod, Cookie}

case class MultiPartRequest(
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
  body: MultiPartBody
)
extends HttpRequest[MultiPartBody]


