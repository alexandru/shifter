package shifter.web.api.responses

import twirl.api.Html
import java.io.InputStream
import javax.servlet.http.HttpServletResponse
import org.apache.commons.codec.binary.Base64
import scala.concurrent.Future

trait ResponseBuilders {
  def Ok(body: String, contentType: String = "", headers: Map[String, String] = Map.empty): SimpleResponse = {
    val headersMulti = if (!contentType.isEmpty)
      headers.updated("Content-Type", contentType)
        .map(kv => (kv._1, Seq(kv._2)))
    else
      headers.map(kv => (kv._1, Seq(kv._2)))

    SimpleResponse(200, body = body, headers = headersMulti)
  }

  def Ok(body: Html): SimpleResponse = {
    val html = body.toString()
    SimpleResponse(200, body=html, headers=Map("Content-Type" -> Seq(body.contentType)))
  }

  def OkStream(body: InputStream, headers: Map[String, String] = Map.empty): StreamResponse =
    StreamResponse(200, body = body, headers = headers.map(kv => (kv._1, Seq(kv._2))))

  def Redirect(location: String, status: Int = 302, headers: Map[String, String] = Map.empty) =
    SimpleResponse(status).withHeader("Location" -> location)

  def HttpError(status: Int, body: String = "", headers: Map[String, String] = Map.empty) = {
    assert(status != 200 && status != 302 && status != 303)
    SimpleResponse(status, headers = headers.map(kv => (kv._1, Seq(kv._2))), body = body)
  }

  def BadRequest(body: String = "", headers: Map[String, String] = Map.empty): SimpleResponse =
    HttpError(HttpServletResponse.SC_BAD_REQUEST, body, headers)

  val BadRequest: SimpleResponse = BadRequest("", Map.empty)

  def NoContent(body: String = "", headers: Map[String, String] = Map.empty): SimpleResponse =
    HttpError(HttpServletResponse.SC_NO_CONTENT, body, headers)

  val NoContent: SimpleResponse = NoContent("", Map.empty)

  def RequestTimeout(body: String = "", headers: Map[String, String] = Map.empty): SimpleResponse =
    HttpError(HttpServletResponse.SC_REQUEST_TIMEOUT, body, headers)

  val RequestTimeout: SimpleResponse = RequestTimeout("", Map.empty)

  def NotFound(body: String = "", headers: Map[String, String] = Map.empty): SimpleResponse =
    HttpError(HttpServletResponse.SC_NOT_FOUND, body, headers)

  val NotFound: SimpleResponse = NotFound("", Map.empty)

  def MethodNotAllowed(body: String = "", headers: Map[String, String] = Map.empty): SimpleResponse =
    HttpError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, body, headers)

  val MethodNotAllowed: SimpleResponse = MethodNotAllowed("", Map.empty)

  def Unauthenticated(realm: String, headers: Map[String, String] = Map.empty) =
    HttpError(401, body = "Unauthenticated", headers = Map(
      "WWW-Authenticate" -> "Basic realm=\"%s\"".format(realm)
    ) ++ headers)

  val Pixel = {
    val pixel = "R0lGODlhAQABAPAAAAAAAAAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw"
    val bytes = Base64.decodeBase64(pixel)

    BytesResponse(200, body = bytes.toSeq)
      .withHeader("Content-type", "image/gif")
  }
}

object ResponseBuilders extends ResponseBuilders
