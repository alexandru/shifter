package shifter.web.api.mvc

import language.existentials
import shifter.web.api.requests._
import shifter.web.api.responses.HttpResponse
import shifter.web.api.responses.ResponseBuilders._

final class Action[T, U <: HttpRequest[T]] private[mvc] (parser: RequestParser[T, U]) {
  def apply(cb: U => HttpResponse[_]): ActionResponse =
    (raw: RawRequest) => {
      if (parser.canBeParsed(raw))
        try
          parser.parse(raw) match {
            case Some(req) =>
              cb(req)
            case None =>
              BadRequest
          }
        catch {
          case ex: ParserException =>
            ex.response
        }
      else
        BadRequest
    }
}

object Action {
  def apply[T](cb: => HttpResponse[_]): ActionResponse =
    (req: RawRequest) => cb

  def apply[T](cb: SimpleRequest => HttpResponse[T]): ActionResponse =
    (req: RawRequest) => cb(SimpleParser.parse(req).get)

  def apply[T, U <: HttpRequest[T]](parser: RequestParser[T, U]) =
    new Action[T, U](parser)
}
