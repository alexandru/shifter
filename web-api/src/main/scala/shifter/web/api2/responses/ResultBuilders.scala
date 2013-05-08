package shifter.web.api2.responses

import language.existentials
import twirl.api.Html
import java.io.InputStream
import javax.servlet.http.HttpServletResponse
import org.apache.commons.codec.binary.Base64
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import shifter.web.api2.http.HeaderNames._
import shifter.web.api2.http.MimeTypes._


trait ResultBuilders {
  val Ok: SimpleResult =
    SimpleResult(200, body = "", headers = Map(
      CONTENT_TYPE -> Seq(TEXT)
    ))

  def Ok(body: String, contentType: String = "", headers: Map[String, String] = Map.empty): SimpleResult = {
    val headersMulti = if (!contentType.isEmpty)
      headers.updated("Content-Type", contentType)
        .map(kv => (kv._1, Seq(kv._2)))
    else
      headers.map(kv => (kv._1, Seq(kv._2)))

    SimpleResult(200, body = body, headers = headersMulti)
  }

  def Ok(body: Html): SimpleResult = {
    val html = body.toString()
    SimpleResult(200, body=html, headers=Map("Content-Type" -> Seq(body.contentType)))
  }

  def OkStream(body: InputStream, headers: Map[String, String] = Map.empty): StreamedResult =
    StreamedResult(200, body = body, headers = headers.map(kv => (kv._1, Seq(kv._2))))

  def Redirect(location: String, status: Int = 302, headers: Map[String, String] = Map.empty) =
    SimpleResult(status).withHeader("Location" -> location)

  def HttpError(status: Int, body: String = "", headers: Map[String, String] = Map.empty) = {
    assert(status != 200 && status != 302 && status != 303)
    SimpleResult(status, headers = headers.map(kv => (kv._1, Seq(kv._2))), body = body)
  }

  def BadRequest(body: String = "", headers: Map[String, String] = Map.empty): SimpleResult =
    HttpError(HttpServletResponse.SC_BAD_REQUEST, body, headers)

  val BadRequest: SimpleResult = BadRequest("", Map.empty)

  def NoContent(body: String = "", headers: Map[String, String] = Map.empty): SimpleResult =
    HttpError(HttpServletResponse.SC_NO_CONTENT, body, headers)

  val NoContent: SimpleResult = NoContent("", Map.empty)

  def RequestTimeout(body: String = "", headers: Map[String, String] = Map.empty): SimpleResult =
    HttpError(HttpServletResponse.SC_REQUEST_TIMEOUT, body, headers)

  val RequestTimeout: SimpleResult = RequestTimeout("", Map.empty)

  def NotFound(body: String = "", headers: Map[String, String] = Map.empty): SimpleResult =
    HttpError(HttpServletResponse.SC_NOT_FOUND, body, headers)

  val NotFound: SimpleResult = NotFound("", Map.empty)

  def MethodNotAllowed(body: String = "", headers: Map[String, String] = Map.empty): SimpleResult =
    HttpError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, body, headers)

  val MethodNotAllowed: SimpleResult = MethodNotAllowed("", Map.empty)

  def Unauthenticated(realm: String, headers: Map[String, String] = Map.empty) =
    HttpError(401, body = "Unauthenticated", headers = Map(
      "WWW-Authenticate" -> "Basic realm=\"%s\"".format(realm)
    ) ++ headers)

  val Pixel = {
    val pixel = "R0lGODlhAQABAPAAAAAAAAAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw"
    val bytes = Base64.decodeBase64(pixel)

    BytesResult(200, body = bytes.toSeq)
      .withHeader("Content-type", "image/gif")
  }

  def Async(response: Future[CompleteResult])(implicit ec: ExecutionContext) =
    AsyncResult(response, ec)

  def Async(response: Future[CompleteResult], timeout: Duration)(implicit ec: ExecutionContext) =
    AsyncResult(response, ec, timeout)

  def Async(response: Future[CompleteResult], timeout: Duration, timeoutResponse: CompleteResult)(implicit ec: ExecutionContext) =
    AsyncResult(response, ec, timeout, timeoutResponse)
}

object ResultBuilders extends ResultBuilders
