package shifter.web.api2.requests

import shifter.web.api2.http.{Cookie, HttpMethod}

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
  body: A
) extends Request[A]
