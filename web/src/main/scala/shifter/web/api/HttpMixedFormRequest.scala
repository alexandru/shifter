package shifter.web.api

import shifter.web.api._
import scala.Some
import util.Try

case class HttpMixedFormRequest(
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


object HttpMixedFormRequest extends RequestParser[Map[String, Seq[String]], HttpMixedFormRequest] {

  def canBeParsed(raw: HttpRawRequest): Boolean =
    validMethods(raw.method)

  def parse(raw: HttpRawRequest): Option[HttpMixedFormRequest] =
    if (canBeParsed(raw)) {
      val bodyString = Try(raw.bodyAsString).getOrElse("")

      val postParams =
        if (bodyString.isEmpty)
          Map.empty[String, Seq[String]]
        else
          urlDecodeMulti(bodyString)

      val params = raw.queryParams ++ postParams

      Some(HttpMixedFormRequest(
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
    HttpMethod.HEAD,
    HttpMethod.OPTIONS,
    HttpMethod.GET,
    HttpMethod.POST,
    HttpMethod.PUT
  )
}
