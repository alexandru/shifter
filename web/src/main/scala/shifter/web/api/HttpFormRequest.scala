package shifter.web.api

case class HttpFormRequest(
  method: HttpMethod.Value,
  path: String,
  domain: String,
  port: Int,
  protocol: String,
  url: String,
  query: Option[String],
  headers: Map[String, Seq[String]],
  remoteAddress: String,
  userInfo: UserInfo,
  cookies: Map[String, Cookie],
  body: Map[String, Seq[String]]
)
extends HttpRequest[Map[String, Seq[String]]] {
  def param(key: String): Option[String] =
    paramsSimple.get(key)

  def paramList(key: String): Seq[String] =
    body.get(key).getOrElse(Seq.empty)

  lazy val paramsSimple: Map[String, String] =
    body.flatMap {
      case (key, list) =>
        list.headOption.map(value => (key, value))
    }
}


object HttpFormRequest extends RequestParser[Map[String, Seq[String]], HttpFormRequest] {

  def canBeParsed(raw: HttpRawRequest): Boolean =
    validMethods(raw.method) && validContentTypes(raw.contentType)

  def parse(raw: HttpRawRequest): Option[HttpFormRequest] =
    if (canBeParsed(raw)) {
      val bodyString = raw.bodyAsString

      val params =
        if (bodyString.isEmpty)
          Map.empty[String, Seq[String]]
        else
          urlDecodeMulti(bodyString)

      Some(HttpFormRequest(
        method = raw.method,
        path = raw.path,
        domain = raw.domain,
        port = raw.port,
        protocol = raw.protocol,
        url = raw.url,
        query = raw.query,
        headers = raw.headers,
        remoteAddress = raw.remoteAddress,
        userInfo = raw.userInfo,
        cookies = raw.cookies,
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
