package shifter.web.api

trait RequestParser[T, U <: HttpRequest[T]] {
  def canBeParsed(raw: HttpRawRequest): Boolean

  def parse(raw: HttpRawRequest): Option[U]
}

