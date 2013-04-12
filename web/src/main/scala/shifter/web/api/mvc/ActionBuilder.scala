package shifter.web.api.mvc

import shifter.web.api.requests.{ParserException, RawRequest, RequestParser, HttpRequest}
import shifter.web.api.responses.HttpResponse
import shifter.web.api.responses.ResponseBuilders._


final class ActionBuilder[T, U <: HttpRequest[T]] private[mvc] (parser: RequestParser[T, U]) {
  def apply(cb: U => HttpResponse[_]): Action =
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
