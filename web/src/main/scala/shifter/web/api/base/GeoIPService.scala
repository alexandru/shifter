package shifter.web.api.base

import shifter.geoip.{GeoIPLocation, GeoIP}
import shifter.http.client.{HttpClientConfig, NingHttpClient}
import util.Try

object GeoIPService {
  def search(address: String): Option[GeoIPLocation] =
    if (address == null || address.isEmpty || address == "127.0.0.1" || address == "0.0.0.0")
      None
    else
      instance.search(address)

  private[this] lazy val instance = {
    val client = NingHttpClient(HttpClientConfig(
      maxTotalConnections = 3,
      maxConnectionsPerHost = 1
    ))

    try {
      GeoIP.withLiteCity(client)
    }
    finally {
      Try(client.close())
    }
  }
}
