package shifter.web.api.utils

import concurrent.{ExecutionContext, Future}
import shifter.web.api.mvc._
import shifter.web.api.http.HttpMethod
import shifter.web.api.requests._
import shifter.web.api.responses._
import shifter.web.api.responses.AsyncResponse
import scala.Some

trait CorsSupport extends ResponseBuilders {
  def CorsAction(cb: => HttpResponse[_])(implicit ec: ExecutionContext): Action =
    (req: RawRequest) =>
      if (req.method == HttpMethod.OPTIONS)
        optionsResponse

      else
        cb match {
          case r: CompleteResponse[_] =>
            r.withHeaders(actionCorsHeaders)

          case AsyncResponse(response, timeout, timeoutResponse) =>
            val newResponse = response.map(_.withHeaders(actionCorsHeaders))
            val newTimeoutResponse = timeoutResponse.withHeaders(actionCorsHeaders)
            Async(newResponse, timeout, newTimeoutResponse)

          case other =>
            other
        }

  def CorsAction(cb: SimpleRequest => HttpResponse[_])(implicit ec: ExecutionContext): Action =
    (req: RawRequest) =>
      if (req.method == HttpMethod.OPTIONS)
        optionsResponse

      else
        cb(SimpleParser.parse(req).get) match {
          case r: CompleteResponse[_] =>
            r.withHeaders(actionCorsHeaders)

          case AsyncResponse(response, timeout, timeoutResponse) =>
            val newResponse = response.map(_.withHeaders(actionCorsHeaders))
            val newTimeoutResponse = timeoutResponse.withHeaders(actionCorsHeaders)
            Async(newResponse, timeout, newTimeoutResponse)

          case other =>
            other
        }

  def CorsAction[T, U <: HttpRequest[T]](parser: RequestParser[T, U])(cb: U => HttpResponse[_])(implicit ec: ExecutionContext): Action =
    (raw: RawRequest) => {
      if (raw.method == HttpMethod.OPTIONS)
        optionsResponse

      else if (parser.canBeParsed(raw))
        try
          parser.parse(raw) match {
            case Some(req) =>
              cb(req)  match {
                case r: CompleteResponse[_] =>
                  r.withHeaders(actionCorsHeaders)

                case AsyncResponse(response, timeout, timeoutResponse) =>
                  val newResponse = response.map(_.withHeaders(actionCorsHeaders))
                  val newTimeoutResponse = timeoutResponse.withHeaders(actionCorsHeaders)
                  Async(newResponse, timeout, newTimeoutResponse)

                case other =>
                  other
              }
            case None =>
              BadRequest.withHeaders(actionCorsHeaders)
          }
        catch {
          case ex: ParserException =>
            ex.response.withHeaders(actionCorsHeaders)
        }

      else
        BadRequest.withHeaders(actionCorsHeaders)
    }

  private[this] val actionCorsHeaders = Map(
    "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS, HEAD, PUT, DELETE",
    "Access-Control-Allow-Credentials" -> "true",
    "Access-Control-Allow-Headers" -> "ORIGIN, CONTENT-TYPE, X-REQUESTED-WITH, *",
    "Access-Control-Allow-Origin" -> "*",
    "Access-Control-Max-Age" -> "30000"
  )

  private[this] val optionsCorsHeaders = actionCorsHeaders ++ Map(
    "Content-Type" -> "text/plain",
    "Allow" -> "GET, POST, OPTIONS, HEAD, PUT, DELETE"
  )

  private[this] val optionsResponse = Ok.withHeaders(optionsCorsHeaders)
}
