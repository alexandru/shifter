package shifter.web.api.requests

import shifter.web.api.base._
import scala.Some
import shifter.web.api.base.UserInfo
import shifter.web.api.base.HeaderNames._


trait HttpRequest[T] {
  def method: HttpMethod.Value
  def path: String
  def domain: String
  def port: Int
  def protocol: String
  def url: String
  def query: Option[String]
  def headers: Map[String, Seq[String]]
  def remoteAddress: String
  def cookies: Map[String, Cookie]

  private[api] def canForward: Boolean = false

  def header(key: String): Option[String] =
    headers.get(key.toUpperCase).flatMap(_.headOption).flatMap {
      str => if (str == null || str.trim.isEmpty) None else Some(str.trim)
    }

  def headerList(key: String): Seq[String] =
    headers.get(key.toUpperCase).getOrElse(Seq.empty)

  lazy val queryParams: Map[String, Seq[String]] =
    this.query match {
      case Some(q) =>
        utils.urlDecodeMulti(q)
      case None =>
        Map.empty
    }

  def queryParam(key: String, default: Option[String] = None): Option[String] =
    queryParams.get(key).flatMap(_.headOption.orElse(default))

  def queryParamList(key: String): Seq[String] =
    queryParams.get(key).getOrElse(Seq.empty)

  lazy val fullUrl: String =
    query match {
      case Some(q) => url + "?" + q
      case None => url
    }

  lazy val remoteForwardedFor = header(X_FORWARDED_FOR).getOrElse("")

  lazy val remoteVia = header(VIA).getOrElse("")

  lazy val remoteRealIP =
    header(X_FORWARDED_FOR) match {
      case Some(value) if !value.isEmpty =>
        value.split("\\s*,\\s*").find(IPUtils.isIPv4Public) match {
          case Some(ip) => ip
          case None =>
            remoteAddress
        }
      case _ =>
        remoteAddress
    }

  lazy val userAgent =
    header(USER_AGENT).getOrElse("")

  lazy val contentType = {
    headers.get(CONTENT_TYPE)
      .flatMap(_.headOption)
      .getOrElse("")
      .split(";")
      .head
      .toLowerCase
  }

  final lazy val userInfo = UserInfo(
    ip = remoteRealIP,
    forwardedFor = remoteForwardedFor,
    via = remoteVia,
    agent = userAgent,
    geoip = GeoIPService.search(remoteRealIP)
  )
}


