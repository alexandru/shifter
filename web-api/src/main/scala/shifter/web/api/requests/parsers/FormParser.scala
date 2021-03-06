package shifter.web.api.requests.parsers

import shifter.web.api.http.HttpMethod
import shifter.web.api.utils
import shifter.web.api.responses._
import shifter.web.api.requests._

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