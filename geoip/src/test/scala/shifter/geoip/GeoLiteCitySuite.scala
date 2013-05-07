package shifter.geoip

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GeoLiteCitySuite extends FunSuite {
  test("lookup") {

    val geoip = GeoIP.withLiteCity()
    val location = geoip.search("188.26.33.60")

    assert(location.isDefined)

    val loc = location.get

    assert(loc.countryCode === "ro")
    assert(loc.countryCode3 === "rou")
    assert(loc.city === Some("Bucharest"))
    assert(loc.countryName === Some("Romania"))
  }
}
