package shifter.web.api

import java.net.URLEncoder
import java.util.{Calendar, TimeZone, Date}


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
        val dt = convertTSToGMTDate(System.currentTimeMillis() + ts * 1000)
        dateFormatter.format(dt) + " GMT"
      }
  }

  private[this] def convertTSToGMTDate(millis: Long): Date = {
    val tz: TimeZone = TimeZone.getDefault
    val c: Calendar = Calendar.getInstance(tz)
    var localMillis: Long = millis
    var offset: Int = 0
    var time: Int = 0
    c.set(1970, Calendar.JANUARY, 1, 0, 0, 0)
    while (localMillis > Integer.MAX_VALUE) {
      c.add(Calendar.MILLISECOND, Integer.MAX_VALUE)
      localMillis -= Integer.MAX_VALUE
    }
    c.add(Calendar.MILLISECOND, localMillis.asInstanceOf[Int])
    time = c.get(Calendar.MILLISECOND)
    time += c.get(Calendar.SECOND) * 1000
    time += c.get(Calendar.MINUTE) * 60 * 1000
    time += c.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000
    offset = tz.getOffset(c.get(Calendar.ERA), c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.DAY_OF_WEEK), time)
    new Date(millis - offset)
  }

  private[this] val dateFormatter =
    new java.text.SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss")
}