package shifter.web

import java.net.URLDecoder._
import java.io.InputStream
import javax.servlet.http.HttpServletResponse
import org.apache.commons.codec.binary.Base64
import twirl.api.Html

package object api {
  def Ok(body: String, contentType: String = "", headers: Map[String, String] = Map.empty): HttpSimpleResponse = {
    val headersMulti = if (!contentType.isEmpty)
      headers.updated("Content-Type", contentType)
        .map(kv => (kv._1, Seq(kv._2)))
    else
      headers.map(kv => (kv._1, Seq(kv._2)))

    HttpSimpleResponse(200, body = body, headers = headersMulti)
  }

  def Ok(body: Html): HttpSimpleResponse = {
    val html = body.toString()
    HttpSimpleResponse(200, body=html, headers=Map("Content-Type" -> Seq(body.contentType)))
  }

  def OkStream(body: InputStream, headers: Map[String, String] = Map.empty): HttpStreamedResponse =
    HttpStreamedResponse(200, body = body, headers = headers.map(kv => (kv._1, Seq(kv._2))))

  def Redirect(location: String, status: Int = 302, headers: Map[String, String] = Map.empty) =
    HttpSimpleResponse(status).withHeader("Location" -> location)

  def HttpError(status: Int, body: String = "", headers: Map[String, String] = Map.empty) = {
    assert(status != 200 && status != 302 && status != 303)
    HttpSimpleResponse(status, headers = headers.map(kv => (kv._1, Seq(kv._2))), body = body)
  }

  def HttpBadRequest(body: String = "", headers: Map[String, String] = Map.empty) =
    HttpError(HttpServletResponse.SC_BAD_REQUEST, body, headers)

  def HttpNoContent(body: String = "", headers: Map[String, String] = Map.empty) =
    HttpError(HttpServletResponse.SC_NO_CONTENT, body, headers)

  def HttpRequestTimeout(body: String = "", headers: Map[String, String] = Map.empty) =
    HttpError(HttpServletResponse.SC_REQUEST_TIMEOUT, body, headers)

  def HttpNotFound(body: String = "", headers: Map[String, String] = Map.empty) =
    HttpError(HttpServletResponse.SC_NOT_FOUND, body, headers)

  def HttpMethodNotAllowed(body: String = "", headers: Map[String, String] = Map.empty) =
    HttpError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, body, headers)

  def HttpUnauthenticated(realm: String, headers: Map[String, String] = Map.empty) =
    HttpError(401, body = "Unauthenticated", headers = Map(
      "WWW-Authenticate" -> "Basic realm=\"%s\"".format(realm)
    ) ++ headers)

  val Pixel = {
    val pixel = "R0lGODlhAQABAPAAAAAAAAAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw"
    val bytes = Base64.decodeBase64(pixel)

    HttpBytesResponse(200, body = bytes.toSeq)
      .withHeader("Content-type", "image/gif")
  }

  val IPFormat = "^(\\d{1,3}[.]\\d{1,3}[.]\\d{1,3}[.]\\d{1,3})$".r

  def urlDecode(data: String): Map[String, String] = {
    if (data != null && !data.isEmpty) {
      val parts = Seq(data.split("&"): _*)
      val values = parts.map(x => Seq(x.split("="): _*)).collect {
        case k :: v :: Nil => (decode(k, "UTF-8"), decode(v, "UTF-8"))
        case k :: Nil => (decode(k, "UTF-8"), "")
      }
      values.toMap
    }
    else
      Map.empty
  }

  def urlDecodeMulti(data: String): Map[String, Seq[String]] = {
    if (data != null && !data.isEmpty) {
      val parts = Seq(data.split("&"): _*)

      val keyVals = parts.map(x => Seq(x.split("="): _*)).collect {
        case k :: v :: Nil => (decode(k, "UTF-8"), decode(v, "UTF-8"))
        case k :: Nil => (decode(k, "UTF-8"), "")
      }

      keyVals.groupBy(_._1).map {
        case (k, list) => (k, list.map(_._2))
      }
    }
    else
      Map.empty
  }

  implicit class RawRequestParsers(val raw: HttpRawRequest)
    extends AnyVal with Parsers
}
