package shifter.web.api2.requests.parsers

import shifter.web.api2.requests.{RequestHeader, AnyContent, BodyParser}
import shifter.web.api2.responses.Result

object AnyContentParser extends BodyParser[AnyContent] {
  def apply(rh: RequestHeader): Either[Result, AnyContent] =
    Right(AnyContent(rh))
}
