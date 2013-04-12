package shifter.web.api.requests

import shifter.web.api.base.{HttpMethod, utils}

object FormParser extends RequestParser[Map[String, Seq[String]], FormRequest] {
  def canBeParsed(raw: RawRequest): Boolean =
    validMethods(raw.method) && (raw.contentType.isEmpty || validContentTypes(raw.contentType))

  def parse(raw: RawRequest): Option[FormRequest] =
    if (canBeParsed(raw)) {
      val bodyString = raw.bodyAsString

      val params =
        if (bodyString.isEmpty)
          Map.empty[String, Seq[String]]
        else
          utils.urlDecodeMulti(bodyString)

      Some(FormRequest(
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
        canForward = raw.canForward,
        body = params
      ))
    }
    else
      None

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

