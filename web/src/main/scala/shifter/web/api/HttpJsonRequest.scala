package shifter.web.api

import spray.json._

case class HttpJsonRequest(
  method: HttpMethod.Value,
  path: String,
  domain: String,
  port: Int,
  protocol: String,
  url: String,
  query: Option[String],
  headers: Map[String, Seq[String]],
  remoteAddress: String,
  cookies: Map[String, Cookie],
  body: JsValue
)
extends HttpRequest[JsValue]


object HttpJsonRequest extends RequestParser[JsValue, HttpJsonRequest] {
  val validMethods = Set(
    HttpMethod.POST,
    HttpMethod.PUT
  )

  val validContentTypes = Set(
    "text/plain",
    "application/json"
  )

  def canBeParsed(raw: HttpRawRequest): Boolean =
    validMethods(raw.method) && validContentTypes(raw.contentType)

  def parse(raw: HttpRawRequest): Option[HttpJsonRequest] =
    if (canBeParsed(raw)) {
      val bodyString = raw.bodyAsString
      val json = JsonParser(bodyString)

      Some(HttpJsonRequest(
        method = raw.method,
        path = raw.path,
        domain = raw.domain,
        port = raw.port,
        protocol = raw.protocol,
        url = raw.url,
        query = raw.query,
        headers = raw.headers,
        remoteAddress = raw.remoteAddress,
        cookies = raw.cookies,
        body = json
      ))
    }
    else
      None
}