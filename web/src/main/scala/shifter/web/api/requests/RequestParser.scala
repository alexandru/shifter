package shifter.web.api.requests

trait RequestParser[T, U <: HttpRequest[T]] {
  def canBeParsed(raw: RawRequest): Boolean

  def parse(raw: RawRequest): Option[U]
}

trait LazyRequestParser[T, U <: HttpRequest[T]]
  extends RequestParser[Option[U], LazyRequest[T, U]]
