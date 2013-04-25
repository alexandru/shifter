package shifter.web.api.utils

import shifter.geoip.{GeoIPLocation, GeoIP}

object GeoIPService {
  def search(address: String): Option[GeoIPLocation] =
    if (address == null || address.isEmpty || address == "127.0.0.1" || address == "0.0.0.0")
      None
    else
      instance.search(address)

  private[this] lazy val instance =
    GeoIP.withLiteCity()
}
