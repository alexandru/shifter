package shifter.geoip

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GeoLiteCitySuite extends FunSuite {
  test("lookup") {

    val geoip = GeoIP.withLiteCity()
    val location = geoip.search("5.12.239.145")

    assert(location.isDefined)

    val loc = location.get

    assert(loc.countryCode === "ro")
    assert(loc.city === Some("Bucharest"))
    assert(loc.countryName === Some("Romania"))
  }
}
