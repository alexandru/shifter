package shifter.json

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.io.{InputStreamReader, BufferedReader}

/**
 * For generating tests:
 *
 * for (i <- 0 until items.length)
 *   println(s"assert(items($i) === ${items(i).toString})")
 */
@RunWith(classOf[JUnitRunner])
class JsonSuite extends FunSuite {
  test("complex json") {
    val jsonSample = readTextFrom("/complex-test.json")
    val items = JsonSax.parse(jsonSample).toVector

    assert(items(0) === JsString(Root \ "app" \ "id", "1231312321"))
    assert(items(1) === JsLong(Root \ "at", 2))
    assert(items(2) === JsString(Root \ "device" \ "dpidsha1", "09dca81da8d12c72c907ae5f839414cd15ece2ac"))
    assert(items(3) === JsString(Root \ "device" \ "geo" \ "country", "US"))
    assert(items(4) === JsDouble(Root \ "device" \ "geo" \ "lat", 45.594555))
    assert(items(5) === JsDouble(Root \ "device" \ "geo" \ "lon", -121.14975))
    assert(items(6) === JsLong(Root \ "device" \ "geo" \ "type", 3))
    assert(items(7) === JsString(Root \ "device" \ "geo" \ "city", "Los Angeles"))
    assert(items(8) === JsString(Root \ "device" \ "geo" \ "region", "CA"))
    assert(items(9) === JsString(Root \ "device" \ "ip", "75.101.145.87"))
    assert(items(10) === JsLong(Root \ "device" \ "js", 1))
    assert(items(11) === JsString(Root \ "device" \ "make", "Apple"))
    assert(items(12) === JsString(Root \ "device" \ "model", "iPhone"))
    assert(items(13) === JsString(Root \ "device" \ "os", "iPhone OS"))
    assert(items(14) === JsString(Root \ "device" \ "ua", "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_2_1 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Mobile/8C148a"))
    assert(items(15) === JsString(Root \ "id", "SGu1Jpq1IO"))
    assert(items(16) === JsLong(Root \ "imp" \ 0 \ "banner" \ "h", 320))
    assert(items(17) === JsString(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 0, "application/javascript"))
    assert(items(18) === JsString(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 1, "image/gif"))
    assert(items(19) === JsString(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 2, "image/jpeg"))
    assert(items(20) === JsString(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 3, "image/png"))
    assert(items(21) === JsString(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 4, "text/html"))
    assert(items(22) === JsString(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 5, "text/javascript"))
    assert(items(23) === JsString(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 6, "text/plain"))
    assert(items(24) === JsLong(Root \ "imp" \ 0 \ "banner" \ "pos", 0))
    assert(items(25) === JsLong(Root \ "imp" \ 0 \ "banner" \ "w", 50))
    assert(items(26) === JsString(Root \ "imp" \ 0 \ "id", "imp-id1"))
    assert(items(27) === JsLong(Root \ "imp" \ 0 \ "instl", 1))
    assert(items(28) === JsLong(Root \ "imp" \ 0 \ "interstitial", 1))
    assert(items(29) === JsDouble(Root \ "imp" \ 0 \ "bidfloor", 0.01))
    assert(items(30) === JsString(Root \ "user" \ "gen", "F"))
    assert(items(31) === JsString(Root \ "user" \ "geo" \ "country", "US"))
    assert(items(32) === JsDouble(Root \ "user" \ "geo" \ "lat", 45.594555))
    assert(items(33) === JsDouble(Root \ "user" \ "geo" \ "lon", -121.14975))
    assert(items(34) === JsLong(Root \ "user" \ "geo" \ "type", 3))
    assert(items(35) === JsString(Root \ "user" \ "geo" \ "zip", "90210"))
    assert(items(36) === JsString(Root \ "user" \ "id", "aON4jWFUT3"))
    assert(items(37) === JsString(Root \ "user" \ "keywords", ""))
    assert(items(38) === JsLong(Root \ "user" \ "yob", 1982))
    assert(items(39) === JsString(Root \ "badv" \ 0, "blocked.ap.ps"))
    assert(items(40) === JsString(Root \ "badv" \ 1, "blocked.epigrams.co"))
    assert(items(41) === JsEnd)
  }

  test("parse arrays") {
    val jsonSample = readTextFrom("/arrays-test.json")
    val items = JsonSax.parse(jsonSample).toVector

    assert(items(0)  === JsLong(Root \ 0 \ 0, 0))
    assert(items(1)  === JsLong(Root \ 0 \ 1, 1))
    assert(items(2)  === JsLong(Root \ 0 \ 2, 2))
    assert(items(3)  === JsLong(Root \ 0 \ 3, 3))
    assert(items(4)  === JsLong(Root \ 0 \ 4, 4))
    assert(items(5)  === JsLong(Root \ 0 \ 5, 5))
    assert(items(6)  === JsLong(Root \ 1 \ 0 \ 0, 0))
    assert(items(7)  === JsLong(Root \ 1 \ 0 \ 1, 1))
    assert(items(8)  === JsLong(Root \ 1 \ 0 \ 2, 2))
    assert(items(9)  === JsLong(Root \ 1 \ 0 \ 3, 3))
    assert(items(10) === JsLong(Root \ 1 \ 0 \ 4, 4))
    assert(items(11) === JsLong(Root \ 1 \ 0 \ 5, 5))
    assert(items(12) === JsLong(Root \ 1 \ 1 \ 0, 0))
    assert(items(13) === JsLong(Root \ 1 \ 1 \ 1, 1))
    assert(items(14) === JsLong(Root \ 1 \ 1 \ 2, 2))
    assert(items(15) === JsLong(Root \ 1 \ 1 \ 3, 3))
    assert(items(16) === JsLong(Root \ 1 \ 1 \ 4, 4))
    assert(items(17) === JsLong(Root \ 1 \ 1 \ 5, 5))
    assert(items(18) === JsLong(Root \ 1 \ 2 \ 0, 0))
    assert(items(19) === JsLong(Root \ 1 \ 2 \ 1, 1))
    assert(items(20) === JsLong(Root \ 1 \ 2 \ 2, 2))
    assert(items(21) === JsLong(Root \ 1 \ 2 \ 3, 3))
    assert(items(22) === JsLong(Root \ 1 \ 2 \ 4, 4))
    assert(items(23) === JsLong(Root \ 1 \ 2 \ 5, 5))
    assert(items(24) === JsLong(Root \ 1 \ 3 \ 0 \ 0, 0))
    assert(items(25) === JsLong(Root \ 1 \ 3 \ 0 \ 1, 1))
    assert(items(26) === JsLong(Root \ 1 \ 3 \ 0 \ 2, 2))
    assert(items(27) === JsLong(Root \ 1 \ 3 \ 0 \ 3, 3))
    assert(items(28) === JsLong(Root \ 1 \ 3 \ 0 \ 4, 4))
    assert(items(29) === JsLong(Root \ 1 \ 3 \ 0 \ 5, 5))
    assert(items(30) === JsLong(Root \ 1 \ 3 \ 2 \ 0, 0))
    assert(items(31) === JsLong(Root \ 1 \ 3 \ 2 \ 1, 1))
    assert(items(32) === JsLong(Root \ 1 \ 3 \ 2 \ 2, 2))
    assert(items(33) === JsLong(Root \ 1 \ 3 \ 2 \ 3, 3))
    assert(items(34) === JsLong(Root \ 1 \ 3 \ 2 \ 4, 4))
    assert(items(35) === JsLong(Root \ 1 \ 3 \ 2 \ 5, 5))
    assert(items(36) === JsLong(Root \ 1 \ 4 \ 0, 0))
    assert(items(37) === JsLong(Root \ 1 \ 4 \ 1, 1))
    assert(items(38) === JsLong(Root \ 1 \ 4 \ 2, 2))
    assert(items(39) === JsLong(Root \ 1 \ 4 \ 3, 3))
    assert(items(40) === JsLong(Root \ 1 \ 4 \ 4, 4))
    assert(items(41) === JsLong(Root \ 1 \ 4 \ 5, 5))
    assert(items(42) === JsLong(Root \ 2 \ 0, 0))
    assert(items(43) === JsLong(Root \ 2 \ 1, 1))
    assert(items(44) === JsLong(Root \ 2 \ 2, 2))
    assert(items(45) === JsLong(Root \ 2 \ 3, 3))
    assert(items(46) === JsLong(Root \ 2 \ 4, 4))
    assert(items(47) === JsLong(Root \ 2 \ 5, 5))
    assert(items(48) === JsEnd)
  }

  test("broken json") {
    val jsonSample = readTextFrom("/broken-json.json")
    val items = JsonSax.parse(jsonSample).toVector

    val Regex = "\\QUnexpected close marker '}': expected ']' (for ARRAY starting at.*".r

    assert(items(0) === JsLong(Root \ "hello" \ 0 \ "world", 1))
    assert(items(1).isInstanceOf[JsError])
    assert(items(1).asInstanceOf[JsError].path === Root \ "hello" \ 0 \ "alex" \ 0)
    assert(!Regex.pattern.matcher(items(1).asInstanceOf[JsError].error).matches())
  }

  test("empty json") {
    val jsonSample = readTextFrom("/empty-test.json")
    JsonSax.parse(jsonSample).toList == List(JsEnd)
  }

  def inputStreamFrom(resource: String) =
    this.getClass.getResourceAsStream(resource)

  def readTextFrom(resource: String): String = {
    val in = new BufferedReader(
      new InputStreamReader(
        this.getClass.getResourceAsStream(resource), "UTF-8"))

    val jsonBuilder = new StringBuilder
    var line = ""
    while (line != null) {
      line = in.readLine()
      if (line != null)
        jsonBuilder.append(line + "\n")
    }

    val value = jsonBuilder.toString()
    in.close()
    value
  }
}
