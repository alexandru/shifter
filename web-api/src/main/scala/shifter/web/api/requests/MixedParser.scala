package shifter.web.api.requests

import scala.util.Try
import shifter.web.api.http.{HttpMethod}
import shifter.web.api.utils

object MixedParser extends RequestParser[Map[String, Seq[String]], MixedRequest] {

  def canBeParsed(raw: RawRequest): Boolean =
    validMethods(raw.method)

  def parse(raw: RawRequest): Option[MixedRequest] =
    if (canBeParsed(raw)) {
      val bodyString = Try(raw.bodyAsString).getOrElse("")

      val postParams =
        if (bodyString.isEmpty)
          Map.empty[String, Seq[String]]
        else
          utils.urlDecodeMulti(bodyString)

      val params = raw.queryParams ++ postParams

      Some(MixedRequest(
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
