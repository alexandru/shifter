package shifter.web.api

import javax.servlet.http.HttpServletRequest
import collection.JavaConverters._


class HttpRawRequest(val underlying: HttpServletRequest)
    extends HttpRequest[HttpServletRequest] {

  private[this] val DomainRegex = "^([^:]+)(?:[:]\\d+)?$".r

  lazy val method: HttpMethod.Value =
    HttpMethod.withName(underlying.getMethod.toUpperCase)

  lazy val path: String =  {
    val servletPath = Option(underlying.getServletPath)
    val pathInfo = Option(underlying.getPathInfo)
    Seq(servletPath, pathInfo).flatten.foldLeft("")(_+_)
  }

  lazy val domain: String = underlying.getServerName match {
    case DomainRegex(name) => name
    case _ => underlying.getServerName
  }

  lazy val port: Int = underlying.getRemotePort

  lazy val protocol = underlying.getProtocol

  lazy val remoteAddress: String = underlying.getRemoteAddr

  lazy val url: String = underlying.getRequestURL.toString

  lazy val query: Option[String] = Option(underlying.getQueryString)

  lazy val headers: Map[String, Seq[String]] =
    underlying.getHeaderNames.asScala.foldLeft(Map.empty[String, Seq[String]]) {
      (acc, key) =>
        val values = underlying.getHeaders(key).asScala.toSeq
        acc.updated(key.toUpperCase, values)
    }

  lazy val cookies: Map[String, Cookie] =
    underlying.getCookies.map { c =>
      val expiresSecs = if (c.getMaxAge < 0)
        None
      else
        Some(c.getMaxAge)

      (c.getName, Cookie(
        name = c.getName,
        value = Option(c.getValue).getOrElse(""),
        path = Option(c.getPath).flatMap(p => if (p.isEmpty) None else Some(p)),
        domain = Option(c.getDomain).flatMap(d => if (d.isEmpty) None else Some(d)),
        expiresSecs = expiresSecs,
        isHttpOnly = c.isHttpOnly,
        isSecure = c.getSecure
      ))
    }
    .toMap

  val body: HttpServletRequest = underlying

  lazy val bodyAsString = {
    val in = body.getReader

    try {
      var line: String = null
      val builder = new java.lang.StringBuilder
      do {
        line = in.readLine
        if (line != null)
          builder.append(line)
      } while (line != null)

      builder.toString
    }
    finally {
      in.close()
    }
  }
}
