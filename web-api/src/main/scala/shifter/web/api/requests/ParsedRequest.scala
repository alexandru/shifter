package shifter.web.api.requests

import shifter.web.api.http.{Cookie, HttpMethod}

final case class ParsedRequest[+A](
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
  contentLength: Int,
  body: A
) extends Request[A]
