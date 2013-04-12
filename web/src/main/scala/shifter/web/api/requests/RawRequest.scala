package shifter.web.api.requests

import javax.servlet.http.HttpServletRequest
import collection.JavaConverters._
import shifter.web.api.base.{HttpMethod, Cookie}
import shifter.units._
import java.nio.charset.Charset


final class RawRequest(underlying: HttpServletRequest)
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

  override private[api] lazy val canForward =
    if (underlying.getContentLength == -1)
      false
    else if (underlying.getContentLength > 5.kilobytes)
      false
    else {
      val in = underlying.getInputStream
      in.markSupported()
    }

  lazy val body: HttpServletRequest = underlying

  lazy val bodyAsString: String =
    if (canForward) {
      val in = underlying.getInputStream
      val length = underlying.getContentLength

      in.mark(length)

      try {
        val in = underlying.getInputStream
        val buf = Array.fill(length)(0.asInstanceOf[Byte])
        var off = 0

        while (off < length) {
          val bytesRead = in.readLine(buf, off, length - off)
          if (bytesRead > 0)
            off += bytesRead
        }

        val enc = underlying.getCharacterEncoding

        if (enc != null && Charset.isSupported(enc))
          new String(buf, 0, off, enc)
        else
          new String(buf, 0, off, "UTF-8")
      }
      finally
        in.reset()
    }
    else {
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
