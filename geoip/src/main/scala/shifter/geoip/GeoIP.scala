package shifter.geoip

import java.io.{BufferedInputStream, FileOutputStream, File}
import com.maxmind.geoip.LookupService
import java.util.zip.GZIPInputStream
import util.Try
import shifter.http.client.{HttpClient, BlockingHttpClient}
import concurrent.{Future, Await}
import concurrent.duration._
import com.typesafe.scalalogging.slf4j.Logging
import shifter.io.Implicits.IOContext


class GeoIP(file: File) {
  lazy val service = {
    new LookupService(file, LookupService.GEOIP_INDEX_CACHE)
  }

  def search(addr: String): Option[GeoIPLocation] =
    Try(Option(service.getLocation(addr))).toOption.flatten.map { loc =>
      GeoIPLocation(
        countryCode = loc.countryCode.toLowerCase,
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
  }
}

object GeoIP extends Logging {
  def withLiteCity(): GeoIP = {
    val client = new BlockingHttpClient
    try {
      withLiteCity(client)
    }
    finally {
      client.close()
    }
  }

  def withLiteCity(httpClient: HttpClient): GeoIP =
    new GeoIP(fetchGeoLiteCity(httpClient))

  private[this] def fetchGeoLiteCity(httpClient: HttpClient): File = {
    val tmpDir = new File(Option(System.getProperty("java.io.tmpdir")).getOrElse("/tmp"))
    val liteCityFile = new File(tmpDir, "Shifter.0.3.14-GeoLiteCity-Auto-jM6gBmhgTop.dat")

    if (!liteCityFile.exists()) {
      logger.info("Fetching GeoLiteCIty.dat")

      val out = new FileOutputStream(liteCityFile)

      try {
        val url1 = "http://geolite.maxmind.com/download/geoip/database/GeoLiteCity.dat.gz"
        val url2 = "http://maven.epigrams.co/data/GeoCityLite.dat.gz"


        val fr1 = httpClient.request("GET", url1).map {
          case resp if resp.status != 200 =>
            throw new RuntimeException("Could not fetch " + url1)
          case resp =>
            logger.info("Fetched GeoLiteCity: " + url1)
            resp
        }
        val fr2 = httpClient.request("GET", url2).map {
          case resp if resp.status != 200 =>
            throw new RuntimeException("Could not fetch " + url2)
          case resp =>
            logger.info("Fetched GeoLiteCity: " + url2)
            resp
        }

        val futureResponse = Future.firstCompletedOf(
          fr1.fallbackTo(fr2) :: fr2.fallbackTo(fr1) :: Nil
        )

        val httpResponse = Await.result(futureResponse, 10.minutes)

        val in = new BufferedInputStream(new GZIPInputStream(httpResponse.bodyAsStream))
        val buffer = new Array[Byte](1024 * 1024)
        var bytes = -1

        do {
          bytes = in.read(buffer)
          if (bytes > 0)
            out.write(buffer, 0, bytes)
        } while (bytes > -1)

        out.close()
        in.close()
      }
      catch {
        case ex: Throwable =>
          Try(liteCityFile.delete())
          throw ex
      }
      finally {
        Try(out.close())
      }
    }

    liteCityFile.deleteOnExit()
    liteCityFile
  }
}
