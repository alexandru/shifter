package shifter.web.api.requests

import java.util.concurrent.atomic.AtomicReference


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

  def get = {
    val opt = inst.get()

    if (opt.isDefined)
      opt.get
    else
      inst.synchronized {
        if (inst.get().isDefined)
          inst.get().get
        else {
          val result = parser.parse(raw)
          inst.set(Some(result))
          result
        }
      }
  }

  private[this] val inst = new AtomicReference(None : Option[Option[U]])
}