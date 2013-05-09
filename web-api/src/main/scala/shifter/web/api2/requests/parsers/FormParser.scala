package shifter.web.api2.requests.parsers

import shifter.web.api2.http.HttpMethod
import shifter.web.api2.utils
import shifter.web.api2.responses._
import shifter.web.api2.requests._

object FormParser extends BodyParser[Map[String, Seq[String]]] with ResultBuilders {
  def canBeParsed(raw: RawRequest): Boolean =
    validMethods(raw.method) && (raw.contentType.isEmpty || validContentTypes(raw.contentType))

  def apply(rh: RequestHeader): Either[Result, Map[String, Seq[String]]] =
    rh match {
      case raw: RawRequest =>
        if (canBeParsed(raw)) {
          val bodyString = raw.bodyAsString

          val params = if (bodyString.isEmpty)
            Map.empty[String, Seq[String]]
          else
            utils.urlDecodeMulti(bodyString)

          Right(params)
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