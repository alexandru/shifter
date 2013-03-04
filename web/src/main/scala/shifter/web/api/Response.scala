package shifter.web.api

import javax.servlet.http.HttpServletResponse
import java.io.InputStream

sealed trait Response

case class HttpOk(
  body: String,
  contentType: String,
  headers: Map[String, String] = Map.empty[String, String]
) extends Response

case class StreamResponse(
  status: Int,
  headers: Map[String, String],
  body: InputStream
) extends Response

case class HttpError(
  status: Int,
  body: String,
  contentType: String,
  headers: Map[String, String] = Map.empty[String, String]
) extends Response

case class HttpRedirect(url: String, status: Int = 302) extends Response
case object Pixel extends Response

abstract class ErrorBuilder(code: Int) {
  def apply(): HttpError =
    HttpError(code, "", "text/plain")
  def apply(msg: String): HttpError =
    HttpError(code, msg, "text/plain")
  def apply(msg: String, contentType: String) =
    HttpError(code, msg, contentType)
  def apply(msg: String, contentType: String, headers: Map[String, String]) =
    HttpError(code, msg, contentType, headers)
}

object HttpBadRequest extends ErrorBuilder(HttpServletResponse.SC_BAD_REQUEST)
object HttpNoContent extends ErrorBuilder(HttpServletResponse.SC_NO_CONTENT)
object HttpRequestTimeout extends ErrorBuilder(HttpServletResponse.SC_REQUEST_TIMEOUT)
object HttpNotFound extends ErrorBuilder(HttpServletResponse.SC_NOT_FOUND)
object HttpMethodNotAllowed extends ErrorBuilder(HttpServletResponse.SC_METHOD_NOT_ALLOWED)

object HttpUnauthenticated {
  def apply(realm: String, headers: Map[String, String] = Map.empty) =
    HttpError(401, "Unauthenticated", "text/plain", Map(
      "WWW-Authenticate" -> "Basic realm=\"%s\"".format(realm)
    ) ++ headers)
}

object HttpOk {
  def apply(body: String): HttpOk =
    HttpOk(body, "text/html")
}
