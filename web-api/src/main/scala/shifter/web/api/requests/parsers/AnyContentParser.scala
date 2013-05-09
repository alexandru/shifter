package shifter.web.api.requests.parsers

import shifter.web.api.requests.{RequestHeader, AnyContent, BodyParser}
import shifter.web.api.responses.Result

object AnyContentParser extends BodyParser[AnyContent] {
  def apply(rh: RequestHeader): Either[Result, AnyContent] =
    Right(AnyContent(rh))
}
