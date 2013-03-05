package shifter.web.api

import java.net.URLEncoder
import java.util.{TimeZone, Date}


case class Cookie(
  name: String,
  value: String,
  path: Option[String] = None,
  domain: Option[String] = None,
  expiresSecs: Option[Int] = None,
  isSecure: Boolean = false,
  isHttpOnly: Boolean = false
)

object Cookie {
  implicit class CookieExtensions(val cookie: Cookie) extends AnyVal {
    def isSession =
      cookie.expiresSecs.isEmpty

    def toHeader: (String, String) =
      ("Set-Cookie", headerValue)

    def headerValue = {
      val value = Some("%s=%s".format(
        URLEncoder.encode(cookie.name, "UTF-8"),
        URLEncoder.encode(cookie.value, "UTF-8")
      ))

      val domainPart = cookie.domain.map(d => "Domain=" + d)
      val pathPart = cookie.path.map(v => "Path=" + v)
      val expiresPart = expiresAsRFC.map(v => "Expires=" + v)
      val securePart = if (cookie.isSecure) Some("Secure") else None
      val httpOnlyPart = if (cookie.isHttpOnly) Some("HttpOnly") else None

      Seq(value, domainPart, pathPart, expiresPart, securePart, httpOnlyPart)
        .flatten.mkString("; ")
    }

    def expiresAsRFC =
      cookie.expiresSecs.map { ts =>
        val dt = new Date(System.currentTimeMillis() / 1000 + ts)
        dateFormatter.format(dt)
      }
  }

  private[this] val dateFormatter = {
    val obj = new java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
    obj.setTimeZone(TimeZone.getTimeZone("GMT"))
    obj
  }
}