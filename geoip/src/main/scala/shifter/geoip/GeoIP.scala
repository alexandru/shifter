package shifter.geoip

import java.io.{FileOutputStream, BufferedOutputStream, File}
import com.maxmind.geoip.LookupService
import util.Try

class GeoIP(resourcePath: String) {
  private[this] val tmpFile = {
    val file = File.createTempFile("geoip", ".dat")
    file.deleteOnExit()
    file
  }

  private[this] val service = {
    val res = this.getClass.getResourceAsStream(resourcePath)
    val out = new BufferedOutputStream(new FileOutputStream(tmpFile))
    val buffer = new Array[Byte](10000)
    var bytes = 0

    do {
      bytes = res.read(buffer)
      if (bytes > 0)
        out.write(buffer, 0, bytes)
    } while (bytes > -1)

    out.flush()
    out.close()
    res.close()

    new LookupService(tmpFile, LookupService.GEOIP_INDEX_CACHE)
  }

  def search(addr: String): Option[GeoIPLocation] =
    Try(Option(service.getLocation(addr))).toOption.flatten.map { loc =>
      GeoIPLocation(
        countryCode = loc.countryCode,
        countryName = Option(loc.countryName),
        city = Option(loc.city),
        latitude = Option(loc.latitude),
        longitude = Option(loc.longitude),
        areaCode = Option(loc.area_code),
        postalCode = Option(loc.postalCode),
        region = Option(loc.region),
        dmaCode = Option(loc.dma_code)
      )
    }

  def close() {
    Try { service.close() }
    Try {
      if (tmpFile.exists())
        tmpFile.delete()
    }
  }
}

object GeoIP {
  def withLiteCity() =
    new GeoIP("/shifter/geoip/GeoLiteCity.dat")
}
