package shifter.web.api

import javax.servlet.http._
import scala.util.Try
import collection.JavaConverters._
import spray.json._


case class Request(underlying: HttpServletRequest) {
  lazy val host: String = {
    val Hostname = "^([^:]+)(?:[:]\\d+)?$".r

    headers.get("Host") match {
      case Some(Hostname(name)) => name
      case _ => ""
    }
  }

  lazy val method: String =
    underlying.getMethod match {
      case "POST" => paramsGetMulti.get("_method") match {
        case Some("PUT" :: Nil) => "PUT"
        case Some("DELETE" :: Nil) => "DELETE"
        case _ => "POST"
      }
      case m => m
    }

  lazy val paramsPostMulti: Map[String, Seq[String]] = {
    val contentType = headers.get("Content-Type").flatMap(_.split(";").headOption.map(_.toLowerCase))
      .getOrElse("")

    val isUrlEncoded = (
      method == "POST" && (
        contentType == "text/plain" || contentType == "application/x-www-form-urlencoded"))

    if (isUrlEncoded)
      urlDecodeMulti(body)
    else
      Map.empty
  }

  lazy val paramsPost: Map[String, String] = {
    val contentType = headers.get("Content-Type").flatMap(_.split(";").headOption.map(_.toLowerCase))
      .getOrElse("")

    val isUrlEncoded = (
      method == "POST" && (
        contentType == "text/plain" || contentType == "application/x-www-form-urlencoded"))

    if (isUrlEncoded)
      urlDecode(body)
    else
      Map.empty
  }

  lazy val paramsGet: Map[String, String] =
    this.query match {
      case Some(q) =>
        urlDecode(q)
      case None =>
        Map.empty
    }

  lazy val paramsGetMulti: Map[String, Seq[String]] =
    this.query match {
      case Some(q) =>
        urlDecodeMulti(q)
      case None =>
        Map.empty
    }

  lazy val params: Map[String, String] =
    paramsGet ++ paramsPost

  lazy val paramsMulti: Map[String, Seq[String]] =
    paramsGetMulti ++ paramsPostMulti

  lazy val headersMulti: Map[String, Seq[String]] =
    underlying.getHeaderNames.asScala.foldLeft(Map.empty[String, Seq[String]]) {
      (acc, key) =>
        val values = underlying.getHeaders(key).asScala.toSeq
        acc.updated(key, Seq(values :_*))
    }

  lazy val headers: Map[String, String] =
    headersMulti.map { case (key, value) => (key, value.head) }

  def queryParamOrDefault(key: String, default: String) =
    paramsGetMulti.get(key).getOrElse(Seq.empty[String])
      .headOption.getOrElse(default)

  lazy val path: String = {
    val servletPath = Option(underlying.getServletPath)
    val pathInfo = Option(underlying.getPathInfo)
    Seq(servletPath, pathInfo).flatten.foldLeft("")(_+_)
  }

  lazy val query: Option[String] =
    Option(underlying.getQueryString)

  lazy val url: String =
    underlying.getRequestURL.toString

  lazy val fullUrl: String = {
    val url = underlying.getRequestURL
    val query = underlying.getQueryString
    if (query != null && !query.isEmpty)
      url.append("?").append(query).toString
    else
      url.toString
  }

  lazy val fullPath: String = {
    val url = this.path
    val query = underlying.getQueryString

    if (query != null && !query.isEmpty)
      url + "?" + query
    else
      url
  }

  lazy val body = {
    val in = underlying.getReader
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

  lazy val bodyAsJson =
    Try(this.body.asJson).getOrElse("{}".asJson)

  lazy val userIP = {
    val userIP = headers.get("CF-Connecting-IP")
      .orElse(headers.get("X-Real-Ip"))
      .getOrElse(underlying.getRemoteAddr)

    headers.get("X-Forwarded-For") match {
      case Some(value) =>
        value.split(',').headOption match {
          case Some(IPFormat(ip)) =>
            ip
          case _ =>
            userIP
        }
      case None =>
        userIP
    }
  }

  lazy val agentHeaders: AgentHeaders = {
    val userIP = this.userIP
    val userAgent = headers.get("User-Agent").getOrElse("")

    if (userIP == "127.0.0.1")
      AgentHeaders(
        ip = userIP,
        userAgent = userAgent,
        via = "",
        forward = userIP
      )
    else
      AgentHeaders(
        ip = userIP,
        userAgent = userAgent,
        via = headers.get("Via").getOrElse(""),
        forward = headers.get("X-Forwarded-For").getOrElse("")
      )
  }
}

case class AgentHeaders(
  ip: String,
  userAgent: String,
  via: String,
  forward: String
)


