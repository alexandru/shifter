package shifter.web.api

import java.util.regex.Pattern

object IPUtils {
  private[this] val _255: String = "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)"
  private[this] val pattern: Pattern = Pattern.compile("^(?:" + _255 + "\\.){3}" + _255 + "$")

  private[this] def ipV4ToLong(ip: String): Long = {
    val octets: Array[String] = ip.split("\\.")
    (java.lang.Long.parseLong(octets(0)) << 24) +
      (Integer.parseInt(octets(1)) << 16) +
      (Integer.parseInt(octets(2)) << 8) + Integer.parseInt(octets(3))
  }

  def isIPv4Private(ip: String): Boolean = {
    val longIp: Long = ipV4ToLong(ip)
    (longIp >= ipV4ToLong("10.0.0.0") &&
      longIp <= ipV4ToLong("10.255.255.255")) ||
      (longIp >= ipV4ToLong("172.16.0.0") && longIp <= ipV4ToLong("172.31.255.255")) ||
      longIp >= ipV4ToLong("192.168.0.0") && longIp <= ipV4ToLong("192.168.255.255")
  }

  def isIPv4Valid(ip: String): Boolean = {
    pattern.matcher(ip).matches
  }

  def isIPv4Public(ip: String) =
    isIPv4Valid(ip) && isIPv4Private(ip)
}
