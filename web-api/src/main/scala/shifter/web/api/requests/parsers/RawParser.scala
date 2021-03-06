package shifter.web.api.requests.parsers

import shifter.web.api.responses.{Result, ResultBuilders}
import shifter.web.api.http.HttpMethod
import java.io.InputStream
import shifter.web.api.requests._

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