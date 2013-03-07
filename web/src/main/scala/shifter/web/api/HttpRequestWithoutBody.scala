package shifter.web.api

case class HttpRequestWithoutBody(
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

object HttpRequestWithoutBody extends RequestParser[None.type, HttpRequestWithoutBody] {
  def canBeParsed(raw: HttpRawRequest): Boolean = true

  def parse(raw: HttpRawRequest): Option[HttpRequestWithoutBody] =
    Some(HttpRequestWithoutBody(
      method = raw.method,
      path = raw.path,
      domain = raw.domain,
      port = raw.port,
      protocol = raw.protocol,
      url = raw.url,
      query = raw.query,
      remoteAddress = raw.remoteAddress,
      headers = raw.headers,
      cookies = raw.cookies
    ))
}

