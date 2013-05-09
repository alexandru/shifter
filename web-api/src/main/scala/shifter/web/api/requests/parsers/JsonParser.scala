package shifter.web.api.requests.parsers

import spray.json.{JsonParser => SprayJsonParser, JsValue}
import shifter.web.api.responses.{Result, ResultBuilders}
import shifter.web.api.http.HttpMethod
import shifter.web.api.requests._



object JsonParser extends BodyParser[JsValue] with ResultBuilders {
  def canBeParsed(raw: RawRequest): Boolean =
    validMethods(raw.method) && (raw.contentType.isEmpty || validContentTypes(raw.contentType))

  def apply(rh: RequestHeader): Either[Result, JsValue] =
    rh match {
      case raw: RawRequest =>
        if (canBeParsed(raw)) {
          val bodyString = raw.bodyAsString
          val json = SprayJsonParser(bodyString)
          Right(json)
        }
        else
          Left(BadRequest)

      case _ =>
        Left(BadRequest)
    }

  val validMethods = Set(
    HttpMethod.POST,
    HttpMethod.PUT,
    HttpMethod.DELETE
  )

  val validContentTypes = Set(
    "text/plain",
    "application/x-www-form-urlencoded"
  )
}