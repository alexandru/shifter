package shifter.web.api


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

  def header(key: String): Option[String] =
    headers.get(key.toUpperCase).flatMap(_.headOption).flatMap {
      str => if (str == null || str.trim.isEmpty) None else Some(str.trim)
    }

  def headerList(key: String): Seq[String] =
    headers.get(key.toUpperCase).getOrElse(Seq.empty)

  lazy val queryParams: Map[String, Seq[String]] =
    this.query match {
      case Some(q) =>
        urlDecodeMulti(q)
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

  lazy val remoteForwardedFor = header("X-FORWARDED-FOR").getOrElse("")

  lazy val remoteVia = header("VIA").getOrElse("")

  lazy val remoteRealIP =
    header("CF-CONNECTING-IP").getOrElse {
      val givenIp = header("X-REAL-IP").getOrElse(remoteAddress)

      if (!remoteForwardedFor.isEmpty)
        remoteForwardedFor.split(',').headOption match {
          case Some(IPFormat(ip)) =>
            ip
          case _ =>
            givenIp
        }
      else
        givenIp
    }

  lazy val userAgent =
    header("USER-AGENT").getOrElse("")

  lazy val contentType = {
    headers.get("CONTENT-TYPE")
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


