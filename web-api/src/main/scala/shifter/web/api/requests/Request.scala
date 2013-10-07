package shifter.web.api.requests


trait Request[+A] extends RequestHeader {
  self =>

  def body: A

  def map[B](f: A => B): Request[B] = new Request[B] {
    def domain = self.domain
    def path = self.path
    def method = self.method
    def port = self.port
    def protocol = self.protocol
    def url = self.url
    def query = self.query
    def headers = self.headers
    def remoteAddress = self.remoteAddress
    def cookies = self.cookies
    def contentLength = self.contentLength
    lazy val body = f(self.body)
  }
}

object Request {
  def apply[A](rh: RequestHeader, body: A): Request[A] = ParsedRequest(
    method = rh.method,
    path = rh.path,
    domain = rh.domain,
    port = rh.port,
    protocol = rh.protocol,
    url = rh.url,
    query = rh.query,
    headers = rh.headers,
    remoteAddress = rh.remoteAddress,
    cookies = rh.cookies,
    contentLength = rh.contentLength,
    body = body
  )
}

