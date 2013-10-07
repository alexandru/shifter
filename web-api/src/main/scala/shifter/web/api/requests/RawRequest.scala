package shifter.web.api.requests

import java.io.InputStream
import javax.servlet.http.HttpServletRequest
import shifter.web.api.http.{Cookie, HttpMethod}
import collection.JavaConverters._
import shifter.units._


trait RawRequest extends Request[InputStream] {
  def body: InputStream
  def bodyAsString: String
}

final class RawServletRequest(val underlying: HttpServletRequest) extends RawRequest {
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

  def contentLength: Int =
    underlying.getContentLength

  lazy val body: InputStream = underlying.getInputStream

  lazy val bodyAsString: String = {
    val in = underlying.getReader
    val contentLength = underlying.getContentLength

    if (contentLength == 0)
      ""
    else if (contentLength > 0 && contentLength < 5.kilobytes) {
      val in = underlying.getReader
      val buffer = new Array[Char](contentLength)
      var charsRead = 0
      var offset = 0

      while (charsRead > -1 && offset < contentLength) {
        charsRead = in.read(buffer, offset, contentLength - offset)
        if (charsRead > 0)
          offset += charsRead
      }

      new String(buffer, 0, offset)
    }
    else
      try {
        val builder = new java.lang.StringBuilder
        val buffer = new Array[Char](512)
        var bytesRead = 0

        while (bytesRead > -1) {
          bytesRead = in.read(buffer, 0, 512)
          if (bytesRead > 0)
            builder.append(buffer, 0, bytesRead)
        }

        builder.toString
      }
      finally {
        in.close()
      }
  }
}