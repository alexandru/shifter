package shifter.web.api.requests.parsers

import shifter.web.api.requests._
import shifter.web.api.responses.Result
import shifter.web.api.http._
import scala.util.control.NonFatal

object StringParser extends BodyParser[String] with HeaderNames {
  def apply(rh: RequestHeader): Either[Result, String] = {
    if (rh.method == HttpMethod.GET || rh.method == HttpMethod.OPTIONS)
      Right("")

    else
      rh match {
        case raw: RawRequest =>
          try
            Right(raw.bodyAsString)
          catch {
            case NonFatal(_) =>
              Right("")
          }
        case _ =>
          Right("")
      }
  }
}
