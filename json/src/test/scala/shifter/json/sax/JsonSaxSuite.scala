package shifter.json.sax

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
class JsonSaxSuite extends FunSuite {
  test("complex json") {
    val jsonSample = readTextFrom("/complex-test.json")
    val items = parseJson(jsonSample).toVector

    assert(items(0) === JsStringEvent(Root \ "app" \ "id", "1231312321"))
    assert(items(1) === JsLongEvent(Root \ "at", 2))
    assert(items(2) === JsStringEvent(Root \ "device" \ "dpidsha1", "09dca81da8d12c72c907ae5f839414cd15ece2ac"))
    assert(items(3) === JsStringEvent(Root \ "device" \ "geo" \ "country", "US"))
    assert(items(4) === JsDoubleEvent(Root \ "device" \ "geo" \ "lat", 45.594555))
    assert(items(5) === JsDoubleEvent(Root \ "device" \ "geo" \ "lon", -121.14975))
    assert(items(6) === JsLongEvent(Root \ "device" \ "geo" \ "type", 3))
    assert(items(7) === JsStringEvent(Root \ "device" \ "geo" \ "city", "Los Angeles"))
    assert(items(8) === JsStringEvent(Root \ "device" \ "geo" \ "region", "CA"))
    assert(items(9) === JsStringEvent(Root \ "device" \ "ip", "75.101.145.87"))
    assert(items(10) === JsLongEvent(Root \ "device" \ "js", 1))
    assert(items(11) === JsStringEvent(Root \ "device" \ "make", "Apple"))
    assert(items(12) === JsStringEvent(Root \ "device" \ "model", "iPhone"))
    assert(items(13) === JsStringEvent(Root \ "device" \ "os", "iPhone OS"))
    assert(items(14) === JsStringEvent(Root \ "device" \ "ua", "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_2_1 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Mobile/8C148a"))
    assert(items(15) === JsStringEvent(Root \ "id", "SGu1Jpq1IO"))
    assert(items(16) === JsLongEvent(Root \ "imp" \ 0 \ "banner" \ "h", 320))
    assert(items(17) === JsStringEvent(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 0, "application/javascript"))
    assert(items(18) === JsStringEvent(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 1, "image/gif"))
    assert(items(19) === JsStringEvent(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 2, "image/jpeg"))
    assert(items(20) === JsStringEvent(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 3, "image/png"))
    assert(items(21) === JsStringEvent(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 4, "text/html"))
    assert(items(22) === JsStringEvent(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 5, "text/javascript"))
    assert(items(23) === JsStringEvent(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 6, "text/plain"))
    assert(items(24) === JsLongEvent(Root \ "imp" \ 0 \ "banner" \ "pos", 0))
    assert(items(25) === JsLongEvent(Root \ "imp" \ 0 \ "banner" \ "w", 50))
    assert(items(26) === JsStringEvent(Root \ "imp" \ 0 \ "id", "imp-id1"))
    assert(items(27) === JsLongEvent(Root \ "imp" \ 0 \ "instl", 1))
    assert(items(28) === JsLongEvent(Root \ "imp" \ 0 \ "interstitial", 1))
    assert(items(29) === JsDoubleEvent(Root \ "imp" \ 0 \ "bidfloor", 0.01))
    assert(items(30) === JsStringEvent(Root \ "user" \ "gen", "F"))
    assert(items(31) === JsStringEvent(Root \ "user" \ "geo" \ "country", "US"))
    assert(items(32) === JsDoubleEvent(Root \ "user" \ "geo" \ "lat", 45.594555))
    assert(items(33) === JsDoubleEvent(Root \ "user" \ "geo" \ "lon", -121.14975))
    assert(items(34) === JsLongEvent(Root \ "user" \ "geo" \ "type", 3))
    assert(items(35) === JsStringEvent(Root \ "user" \ "geo" \ "zip", "90210"))
    assert(items(36) === JsStringEvent(Root \ "user" \ "id", "aON4jWFUT3"))
    assert(items(37) === JsStringEvent(Root \ "user" \ "keywords", ""))
    assert(items(38) === JsLongEvent(Root \ "user" \ "yob", 1982))
    assert(items(39) === JsStringEvent(Root \ "badv" \ 0, "blocked.ap.ps"))
    assert(items(40) === JsStringEvent(Root \ "badv" \ 1, "blocked.epigrams.co"))
    assert(items(41) === JsonEnd)
  }

  test("parse arrays") {
    val jsonSample = readTextFrom("/arrays-test.json")
    val items = parseJson(jsonSample).toVector

    assert(items(0)  === JsLongEvent(Root \ 0 \ 0, 0))
    assert(items(1)  === JsLongEvent(Root \ 0 \ 1, 1))
    assert(items(2)  === JsLongEvent(Root \ 0 \ 2, 2))
    assert(items(3)  === JsLongEvent(Root \ 0 \ 3, 3))
    assert(items(4)  === JsLongEvent(Root \ 0 \ 4, 4))
    assert(items(5)  === JsLongEvent(Root \ 0 \ 5, 5))
    assert(items(6)  === JsLongEvent(Root \ 1 \ 0 \ 0, 0))
    assert(items(7)  === JsLongEvent(Root \ 1 \ 0 \ 1, 1))
    assert(items(8)  === JsLongEvent(Root \ 1 \ 0 \ 2, 2))
    assert(items(9)  === JsLongEvent(Root \ 1 \ 0 \ 3, 3))
    assert(items(10) === JsLongEvent(Root \ 1 \ 0 \ 4, 4))
    assert(items(11) === JsLongEvent(Root \ 1 \ 0 \ 5, 5))
    assert(items(12) === JsLongEvent(Root \ 1 \ 1 \ 0, 0))
    assert(items(13) === JsLongEvent(Root \ 1 \ 1 \ 1, 1))
    assert(items(14) === JsLongEvent(Root \ 1 \ 1 \ 2, 2))
    assert(items(15) === JsLongEvent(Root \ 1 \ 1 \ 3, 3))
    assert(items(16) === JsLongEvent(Root \ 1 \ 1 \ 4, 4))
    assert(items(17) === JsLongEvent(Root \ 1 \ 1 \ 5, 5))
    assert(items(18) === JsLongEvent(Root \ 1 \ 2 \ 0, 0))
    assert(items(19) === JsLongEvent(Root \ 1 \ 2 \ 1, 1))
    assert(items(20) === JsLongEvent(Root \ 1 \ 2 \ 2, 2))
    assert(items(21) === JsLongEvent(Root \ 1 \ 2 \ 3, 3))
    assert(items(22) === JsLongEvent(Root \ 1 \ 2 \ 4, 4))
    assert(items(23) === JsLongEvent(Root \ 1 \ 2 \ 5, 5))
    assert(items(24) === JsLongEvent(Root \ 1 \ 3 \ 0 \ 0, 0))
    assert(items(25) === JsLongEvent(Root \ 1 \ 3 \ 0 \ 1, 1))
    assert(items(26) === JsLongEvent(Root \ 1 \ 3 \ 0 \ 2, 2))
    assert(items(27) === JsLongEvent(Root \ 1 \ 3 \ 0 \ 3, 3))
    assert(items(28) === JsLongEvent(Root \ 1 \ 3 \ 0 \ 4, 4))
    assert(items(29) === JsLongEvent(Root \ 1 \ 3 \ 0 \ 5, 5))
    assert(items(30) === JsLongEvent(Root \ 1 \ 3 \ 2 \ 0, 0))
    assert(items(31) === JsLongEvent(Root \ 1 \ 3 \ 2 \ 1, 1))
    assert(items(32) === JsLongEvent(Root \ 1 \ 3 \ 2 \ 2, 2))
    assert(items(33) === JsLongEvent(Root \ 1 \ 3 \ 2 \ 3, 3))
    assert(items(34) === JsLongEvent(Root \ 1 \ 3 \ 2 \ 4, 4))
    assert(items(35) === JsLongEvent(Root \ 1 \ 3 \ 2 \ 5, 5))
    assert(items(36) === JsLongEvent(Root \ 1 \ 4 \ 0, 0))
    assert(items(37) === JsLongEvent(Root \ 1 \ 4 \ 1, 1))
    assert(items(38) === JsLongEvent(Root \ 1 \ 4 \ 2, 2))
    assert(items(39) === JsLongEvent(Root \ 1 \ 4 \ 3, 3))
    assert(items(40) === JsLongEvent(Root \ 1 \ 4 \ 4, 4))
    assert(items(41) === JsLongEvent(Root \ 1 \ 4 \ 5, 5))
    assert(items(42) === JsLongEvent(Root \ 2 \ 0, 0))
    assert(items(43) === JsLongEvent(Root \ 2 \ 1, 1))
    assert(items(44) === JsLongEvent(Root \ 2 \ 2, 2))
    assert(items(45) === JsLongEvent(Root \ 2 \ 3, 3))
    assert(items(46) === JsLongEvent(Root \ 2 \ 4, 4))
    assert(items(47) === JsLongEvent(Root \ 2 \ 5, 5))
    assert(items(48) === JsonEnd)
  }

  test("broken json") {
    val jsonSample = readTextFrom("/broken-json.json")
    val items = parseJson(jsonSample).toVector

    val Regex = "\\QUnexpected close marker '}': expected ']' (for ARRAY starting at.*".r

    assert(items(0) === JsLongEvent(Root \ "hello" \ 0 \ "world", 1))
    assert(items(1).isInstanceOf[JsonError])
    assert(items(1).asInstanceOf[JsonError].path === Root \ "hello" \ 0 \ "alex" \ 0)
    assert(!Regex.pattern.matcher(items(1).asInstanceOf[JsonError].error).matches())
  }

  test("empty json") {
    val jsonSample = readTextFrom("/empty-test.json")
    parseJson(jsonSample).toList == List(JsonEnd)
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
