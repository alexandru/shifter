package shifter.web.api.mvc

import language.existentials
import shifter.web.api.requests._
import shifter.web.api.responses.HttpResponse

object Action {
  def apply(cb: => HttpResponse[_]): Action =
    (req: RawRequest) => cb

  def apply(cb: SimpleRequest => HttpResponse[_]): Action =
    (req: RawRequest) => cb(SimpleParser.parse(req).get)

  def apply[T, U <: HttpRequest[T]](parser: RequestParser[T, U]) =
    new ActionBuilder[T, U](parser)
}
