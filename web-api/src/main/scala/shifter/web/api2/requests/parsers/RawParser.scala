package shifter.web.api2.requests.parsers

import shifter.web.api2.responses.{Result, ResultBuilders}
import shifter.web.api2.http.HttpMethod
import java.io.InputStream
import shifter.web.api2.requests._

object RawParser extends BodyParser[InputStream] with ResultBuilders {
  def canBeParsed(raw: RawRequest): Boolean =
    validMethods(raw.method)

  def apply(rh: RequestHeader): Either[Result, InputStream] =
    rh match {
      case raw: RawRequest =>
        Right(raw.body)

      case _ =>
        Left(BadRequest)
    }

  val validMethods = Set(
    HttpMethod.HEAD,
    HttpMethod.OPTIONS,
    HttpMethod.GET,
    HttpMethod.POST,
    HttpMethod.PUT
  )
}