package shifter.web.api

import shifter.geoip.{GeoIPLocation, GeoIP}
import java.util.concurrent.atomic.AtomicReference

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
          val obj = GeoIP.withLiteCity()
          _instance.set(Some(obj))
          obj
        }
      }

  private[this] val _instance =
    new AtomicReference(None : Option[GeoIP])
}
