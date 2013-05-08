package shifter.web.api2.requests.parsers

import shifter.web.api2.responses.{Result, ResultBuilders}
import shifter.web.api2.utils
import shifter.web.api2.http.HttpMethod
import scala.util.Try
import shifter.web.api2.requests._

object MixedFormParser extends BodyParser[Map[String, Seq[String]]] with ResultBuilders {
  def canBeParsed(raw: RawRequest): Boolean =
    validMethods(raw.method)

  def apply(rh: RequestHeader): Either[Result, Map[String, Seq[String]]] =
    rh match {
      case raw: RawRequest =>
        if (canBeParsed(raw)) {
          val bodyString = Try(raw.bodyAsString).getOrElse("")

          val postParams =
            if (bodyString.isEmpty)
              Map.empty[String, Seq[String]]
            else
              utils.urlDecodeMulti(bodyString)

          val params = raw.queryParams ++ postParams
          Right(params)
        }
        else
          Left(BadRequest)

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

