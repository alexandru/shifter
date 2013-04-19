package shifter.web.api.requests


class LazyRequest[T, U <: HttpRequest[T]](raw: RawRequest, parser: RequestParser[T, U])
    extends HttpRequest[Option[U]] {

  def method = raw.method
  def path = raw.path
  def domain = raw.domain
  def port = raw.port
  def protocol = raw.protocol
  def url = raw.url
  def query = raw.query
  def headers = raw.headers
  def remoteAddress = raw.remoteAddress
  def cookies = raw.cookies

  lazy val get = parser.parse(raw)
}