package shifter.web.api.base

import shifter.geoip.{GeoIPLocation, GeoIP}
import java.util.concurrent.atomic.AtomicReference
import shifter.http.client.{HttpClientConfig, NingHttpClient}
import util.Try

object GeoIPService {
  def search(address: String): Option[GeoIPLocation] =
    if (address == null || address.isEmpty || address == "127.0.0.1" || address == "0.0.0.0")
      None
    else
      instance.search(address)

  private[this] def instance =
    if (_instance.get().isDefined)
      _instance.get().get
    else
      this.synchronized {
        if (_instance.get().isDefined)
          _instance.get().get
        else {
          val client = NingHttpClient(HttpClientConfig(
            maxTotalConnections = 3,
            maxConnectionsPerHost = 1
          ))

          try {
            val obj = GeoIP.withLiteCity(client)
            _instance.set(Some(obj))
            obj
          }
          finally {
            Try(client.close())
          }
        }
      }

  private[this] val _instance =
    new AtomicReference(None : Option[GeoIP])
}
