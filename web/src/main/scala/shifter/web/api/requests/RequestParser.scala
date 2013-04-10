package shifter.web.api.requests

import shifter.web.api.responses.CompleteResponse

class ParserException(val response: CompleteResponse[_])
  extends RuntimeException

trait RequestParser[T, U <: HttpRequest[T]] {
  def canBeParsed(raw: RawRequest): Boolean

  /**
   * @throws ParserException for signaling errors
   */
  def parse(raw: RawRequest): Option[U]
}

trait LazyRequestParser[T, U <: HttpRequest[T]]
  extends RequestParser[Option[U], LazyRequest[T, U]]

