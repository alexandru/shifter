package shifter.web.api.requests

final class LazyParser[T, U <: HttpRequest[T]](parser: RequestParser[T, U]) extends LazyRequestParser[T, U]  {
  def canBeParsed(raw: RawRequest): Boolean = true

  def parse(raw: RawRequest): Option[LazyRequest[T, U]] =
    Some(new LazyRequest[T, U](raw, parser))
}
