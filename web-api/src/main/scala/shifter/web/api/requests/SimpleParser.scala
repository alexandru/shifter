package shifter.web.api.requests

object SimpleParser extends RequestParser[None.type, SimpleRequest] {
  def canBeParsed(raw: RawRequest): Boolean = true

  def parse(raw: RawRequest): Option[SimpleRequest] =
    Some(SimpleRequest(
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
