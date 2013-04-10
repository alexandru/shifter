package shifter.web.api.requests

import spray.json.{JsonParser => SprayJsonParser, JsValue}
import shifter.web.api.base.HttpMethod

object JsonParser extends RequestParser[JsValue, JsonRequest] {
  val validMethods = Set(
    HttpMethod.POST,
    HttpMethod.PUT
  )

  val validContentTypes = Set(
    "text/plain",
    "application/json"
  )

  def canBeParsed(raw: RawRequest): Boolean =
    validMethods(raw.method) && validContentTypes(raw.contentType)

  def parse(raw: RawRequest): Option[JsonRequest] =
    if (canBeParsed(raw)) {
      val bodyString = raw.bodyAsString
      val json = SprayJsonParser(bodyString)

      Some(JsonRequest(
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