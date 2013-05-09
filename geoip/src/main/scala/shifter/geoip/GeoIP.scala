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
      val countryCode = loc.countryCode.toLowerCase

      GeoIPLocation(
        countryCode = countryCode,
        countryCode3 = GeoIP.iso3Translations.get(countryCode).getOrElse(""),
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

  val iso3Translations = Map(
    "af" -> "afg",
    "al" -> "alb",
    "dz" -> "dza",
    "as" -> "asm",
    "ad" -> "and",
    "ao" -> "ago",
    "ai" -> "aia",
    "aq" -> "ata",
    "ag" -> "atg",
    "ar" -> "arg",
    "am" -> "arm",
    "aw" -> "abw",
    "au" -> "aus",
    "at" -> "aut",
    "az" -> "aze",
    "bs" -> "bhs",
    "bh" -> "bhr",
    "bd" -> "bgd",
    "bb" -> "brb",
    "by" -> "blr",
    "be" -> "bel",
    "bz" -> "blz",
    "bj" -> "ben",
    "bm" -> "bmu",
    "bt" -> "btn",
    "bo" -> "bol",
    "ba" -> "bih",
    "bw" -> "bwa",
    "bv" -> "bvt",
    "br" -> "bra",
    "io" -> "iot",
    "bn" -> "brn",
    "bg" -> "bgr",
    "bf" -> "bfa",
    "bi" -> "bdi",
    "kh" -> "khm",
    "cm" -> "cmr",
    "ca" -> "can",
    "cv" -> "cpv",
    "ky" -> "cym",
    "cf" -> "caf",
    "td" -> "tcd",
    "cl" -> "chl",
    "cn" -> "chn",
    "cx" -> "cxr",
    "cc" -> "cck",
    "co" -> "col",
    "km" -> "com",
    "cg" -> "cog",
    "ck" -> "cok",
    "cr" -> "cri",
    "ci" -> "civ",
    "hr" -> "hrv",
    "cu" -> "cub",
    "cy" -> "cyp",
    "cz" -> "cze",
    "dk" -> "dnk",
    "dj" -> "dji",
    "dm" -> "dma",
    "do" -> "dom",
    "tp" -> "tmp",
    "ec" -> "ecu",
    "eg" -> "egy",
    "sv" -> "slv",
    "gq" -> "gnq",
    "er" -> "eri",
    "ee" -> "est",
    "et" -> "eth",
    "fk" -> "flk",
    "fo" -> "fro",
    "fj" -> "fji",
    "fi" -> "fin",
    "fr" -> "fra",
    "fx" -> "fxx",
    "gf" -> "guf",
    "pf" -> "pyf",
    "tf" -> "atf",
    "ga" -> "gab",
    "gm" -> "gmb",
    "ge" -> "geo",
    "de" -> "deu",
    "gh" -> "gha",
    "gi" -> "gib",
    "gr" -> "grc",
    "gl" -> "grl",
    "gd" -> "grd",
    "gp" -> "glp",
    "gu" -> "gum",
    "gt" -> "gtm",
    "gn" -> "gin",
    "gw" -> "gnb",
    "gy" -> "guy",
    "ht" -> "hti",
    "hm" -> "hmd",
    "va" -> "vat",
    "hn" -> "hnd",
    "hk" -> "hkg",
    "hu" -> "hun",
    "is" -> "isl",
    "in" -> "ind",
    "id" -> "idn",
    "ir" -> "irn",
    "iq" -> "irq",
    "ie" -> "irl",
    "il" -> "isr",
    "it" -> "ita",
    "jm" -> "jam",
    "jp" -> "jpn",
    "jo" -> "jor",
    "kz" -> "kaz",
    "ke" -> "ken",
    "ki" -> "kir",
    "kp" -> "prk",
    "kr" -> "kor",
    "kw" -> "kwt",
    "kg" -> "kgz",
    "la" -> "lao",
    "lv" -> "lva",
    "lb" -> "lbn",
    "ls" -> "lso",
    "lr" -> "lbr",
    "ly" -> "lby",
    "li" -> "lie",
    "lt" -> "ltu",
    "lu" -> "lux",
    "mo" -> "mac",
    "mk" -> "mkd",
    "mg" -> "mdg",
    "mw" -> "mwi",
    "my" -> "mys",
    "mv" -> "mdv",
    "ml" -> "mli",
    "mt" -> "mlt",
    "mh" -> "mhl",
    "mq" -> "mtq",
    "mr" -> "mrt",
    "mu" -> "mus",
    "yt" -> "myt",
    "mx" -> "mex",
    "fm" -> "fsm",
    "md" -> "mda",
    "mc" -> "mco",
    "mn" -> "mng",
    "ms" -> "msr",
    "ma" -> "mar",
    "mz" -> "moz",
    "mm" -> "mmr",
    "na" -> "nam",
    "nr" -> "nru",
    "np" -> "npl",
    "nl" -> "nld",
    "an" -> "ant",
    "nc" -> "ncl",
    "nz" -> "nzl",
    "ni" -> "nic",
    "ne" -> "ner",
    "ng" -> "nga",
    "nu" -> "niu",
    "nf" -> "nfk",
    "mp" -> "mnp",
    "no" -> "nor",
    "om" -> "omn",
    "pk" -> "pak",
    "pw" -> "plw",
    "pa" -> "pan",
    "pg" -> "png",
    "py" -> "pry",
    "pe" -> "per",
    "ph" -> "phl",
    "pn" -> "pcn",
    "pl" -> "pol",
    "pt" -> "prt",
    "pr" -> "pri",
    "qa" -> "qat",
    "re" -> "reu",
    "ro" -> "rou",
    "ru" -> "rus",
    "rw" -> "rwa",
    "kn" -> "kna",
    "lc" -> "lca",
    "vc" -> "vct",
    "ws" -> "wsm",
    "sm" -> "smr",
    "st" -> "stp",
    "sa" -> "sau",
    "sn" -> "sen",
    "sc" -> "syc",
    "sl" -> "sle",
    "sg" -> "sgp",
    "sk" -> "svk",
    "si" -> "svn",
    "sb" -> "slb",
    "so" -> "som",
    "za" -> "zaf",
    "gs" -> "sgs",
    "es" -> "esp",
    "lk" -> "lka",
    "sh" -> "shn",
    "pm" -> "spm",
    "sd" -> "sdn",
    "sr" -> "sur",
    "sj" -> "sjm",
    "sz" -> "swz",
    "se" -> "swe",
    "ch" -> "che",
    "sy" -> "syr",
    "tw" -> "twn",
    "tj" -> "tjk",
    "tz" -> "tza",
    "th" -> "tha",
    "tg" -> "tgo",
    "tk" -> "tkl",
    "to" -> "ton",
    "tt" -> "tto",
    "tn" -> "tun",
    "tr" -> "tur",
    "tm" -> "tkm",
    "tc" -> "tca",
    "tv" -> "tuv",
    "ug" -> "uga",
    "ua" -> "ukr",
    "ae" -> "are",
    "gb" -> "gbr",
    "us" -> "usa",
    "um" -> "umi",
    "uy" -> "ury",
    "uz" -> "uzb",
    "vu" -> "vut",
    "ve" -> "ven",
    "vn" -> "vnm",
    "vg" -> "vgb",
    "vi" -> "vir",
    "wf" -> "wlf",
    "eh" -> "esh",
    "ye" -> "yem",
    "yu" -> "yug",
    "zr" -> "zar",
    "zm" -> "zmb",
    "zw" -> "zwe"
  )
}
