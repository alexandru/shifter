package shifter.web.api.requests

import shifter.web.api.base.{HttpMethod, Cookie}

case class SimpleRequest(
  method: HttpMethod.Value,
  path: String,
  domain: String,
  port: Int,
  protocol: String,
  url: String,
  query: Option[String],
  remoteAddress: String,
  headers: Map[String, Seq[String]],
  cookies: Map[String, Cookie]
)
extends HttpRequest[None.type]

object SimpleRequest {
  implicit val YesItCanForward = new CanForward[SimpleRequest]
}
