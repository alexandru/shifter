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

    assert(items(0) === Event.String(Root \ "app" \ "id", "1231312321"))
    assert(items(1) === Event.Long(Root \ "at", 2))
    assert(items(2) === Event.String(Root \ "device" \ "dpidsha1", "09dca81da8d12c72c907ae5f839414cd15ece2ac"))
    assert(items(3) === Event.String(Root \ "device" \ "geo" \ "country", "US"))
    assert(items(4) === Event.Double(Root \ "device" \ "geo" \ "lat", 45.594555))
    assert(items(5) === Event.Double(Root \ "device" \ "geo" \ "lon", -121.14975))
    assert(items(6) === Event.Long(Root \ "device" \ "geo" \ "type", 3))
    assert(items(7) === Event.String(Root \ "device" \ "geo" \ "city", "Los Angeles"))
    assert(items(8) === Event.String(Root \ "device" \ "geo" \ "region", "CA"))
    assert(items(9) === Event.String(Root \ "device" \ "ip", "75.101.145.87"))
    assert(items(10) === Event.Long(Root \ "device" \ "js", 1))
    assert(items(11) === Event.String(Root \ "device" \ "make", "Apple"))
    assert(items(12) === Event.String(Root \ "device" \ "model", "iPhone"))
    assert(items(13) === Event.String(Root \ "device" \ "os", "iPhone OS"))
    assert(items(14) === Event.String(Root \ "device" \ "ua", "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_2_1 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Mobile/8C148a"))
    assert(items(15) === Event.String(Root \ "id", "SGu1Jpq1IO"))
    assert(items(16) === Event.Long(Root \ "imp" \ 0 \ "banner" \ "h", 320))
    assert(items(17) === Event.String(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 0, "application/javascript"))
    assert(items(18) === Event.String(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 1, "image/gif"))
    assert(items(19) === Event.String(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 2, "image/jpeg"))
    assert(items(20) === Event.String(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 3, "image/png"))
    assert(items(21) === Event.String(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 4, "text/html"))
    assert(items(22) === Event.String(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 5, "text/javascript"))
    assert(items(23) === Event.String(Root \ "imp" \ 0 \ "banner" \ "mimes" \ 6, "text/plain"))
    assert(items(24) === Event.Long(Root \ "imp" \ 0 \ "banner" \ "pos", 0))
    assert(items(25) === Event.Long(Root \ "imp" \ 0 \ "banner" \ "w", 50))
    assert(items(26) === Event.String(Root \ "imp" \ 0 \ "id", "imp-id1"))
    assert(items(27) === Event.Long(Root \ "imp" \ 0 \ "instl", 1))
    assert(items(28) === Event.Long(Root \ "imp" \ 0 \ "interstitial", 1))
    assert(items(29) === Event.Double(Root \ "imp" \ 0 \ "bidfloor", 0.01))
    assert(items(30) === Event.String(Root \ "user" \ "gen", "F"))
    assert(items(31) === Event.String(Root \ "user" \ "geo" \ "country", "US"))
    assert(items(32) === Event.Double(Root \ "user" \ "geo" \ "lat", 45.594555))
    assert(items(33) === Event.Double(Root \ "user" \ "geo" \ "lon", -121.14975))
    assert(items(34) === Event.Long(Root \ "user" \ "geo" \ "type", 3))
    assert(items(35) === Event.String(Root \ "user" \ "geo" \ "zip", "90210"))
    assert(items(36) === Event.String(Root \ "user" \ "id", "aON4jWFUT3"))
    assert(items(37) === Event.String(Root \ "user" \ "keywords", ""))
    assert(items(38) === Event.Long(Root \ "user" \ "yob", 1982))
    assert(items(39) === Event.String(Root \ "badv" \ 0, "blocked.ap.ps"))
    assert(items(40) === Event.String(Root \ "badv" \ 1, "blocked.epigrams.co"))
    assert(items(41) === Event.End)
  }

  test("parse arrays") {
    val jsonSample = readTextFrom("/arrays-test.json")
    val items = parseJson(jsonSample).toVector

    assert(items(0) === Event.Long(Root \ 0 \ 0, 0))
    assert(items(1) === Event.Long(Root \ 0 \ 1, 1))
    assert(items(2) === Event.Long(Root \ 0 \ 2, 2))
    assert(items(3) === Event.Long(Root \ 0 \ 3, 3))
    assert(items(4) === Event.Long(Root \ 0 \ 4, 4))
    assert(items(5) === Event.Long(Root \ 0 \ 5, 5))
    assert(items(6) === Event.Long(Root \ 1 \ 0 \ 0, 0))
    assert(items(7) === Event.Long(Root \ 1 \ 0 \ 1, 1))
    assert(items(8) === Event.Long(Root \ 1 \ 0 \ 2, 2))
    assert(items(9) === Event.Long(Root \ 1 \ 0 \ 3, 3))
    assert(items(10) === Event.Long(Root \ 1 \ 0 \ 4, 4))
    assert(items(11) === Event.Long(Root \ 1 \ 0 \ 5, 5))
    assert(items(12) === Event.Long(Root \ 1 \ 1 \ 0, 0))
    assert(items(13) === Event.Long(Root \ 1 \ 1 \ 1, 1))
    assert(items(14) === Event.Long(Root \ 1 \ 1 \ 2, 2))
    assert(items(15) === Event.Long(Root \ 1 \ 1 \ 3, 3))
    assert(items(16) === Event.Long(Root \ 1 \ 1 \ 4, 4))
    assert(items(17) === Event.Long(Root \ 1 \ 1 \ 5, 5))
    assert(items(18) === Event.Long(Root \ 1 \ 2 \ 0, 0))
    assert(items(19) === Event.Long(Root \ 1 \ 2 \ 1, 1))
    assert(items(20) === Event.Long(Root \ 1 \ 2 \ 2, 2))
    assert(items(21) === Event.Long(Root \ 1 \ 2 \ 3, 3))
    assert(items(22) === Event.Long(Root \ 1 \ 2 \ 4, 4))
    assert(items(23) === Event.Long(Root \ 1 \ 2 \ 5, 5))
    assert(items(24) === Event.Long(Root \ 1 \ 3 \ 0 \ 0, 0))
    assert(items(25) === Event.Long(Root \ 1 \ 3 \ 0 \ 1, 1))
    assert(items(26) === Event.Long(Root \ 1 \ 3 \ 0 \ 2, 2))
    assert(items(27) === Event.Long(Root \ 1 \ 3 \ 0 \ 3, 3))
    assert(items(28) === Event.Long(Root \ 1 \ 3 \ 0 \ 4, 4))
    assert(items(29) === Event.Long(Root \ 1 \ 3 \ 0 \ 5, 5))
    assert(items(30) === Event.Null(Root \ 1 \ 3 \ 1 \ 0))
    assert(items(31) === Event.Null(Root \ 1 \ 3 \ 1 \ 1))
    assert(items(32) === Event.Null(Root \ 1 \ 3 \ 1 \ 2))
    assert(items(33) === Event.Null(Root \ 1 \ 3 \ 1 \ 3))
    assert(items(34) === Event.Null(Root \ 1 \ 3 \ 1 \ 4))
    assert(items(35) === Event.Long(Root \ 1 \ 3 \ 2 \ 0, 0))
    assert(items(36) === Event.Long(Root \ 1 \ 3 \ 2 \ 1, 1))
    assert(items(37) === Event.Long(Root \ 1 \ 3 \ 2 \ 2, 2))
    assert(items(38) === Event.Long(Root \ 1 \ 3 \ 2 \ 3, 3))
    assert(items(39) === Event.Long(Root \ 1 \ 3 \ 2 \ 4, 4))
    assert(items(40) === Event.Long(Root \ 1 \ 3 \ 2 \ 5, 5))
    assert(items(41) === Event.Long(Root \ 1 \ 4 \ 0, 0))
    assert(items(42) === Event.Long(Root \ 1 \ 4 \ 1, 1))
    assert(items(43) === Event.Long(Root \ 1 \ 4 \ 2, 2))
    assert(items(44) === Event.Long(Root \ 1 \ 4 \ 3, 3))
    assert(items(45) === Event.Long(Root \ 1 \ 4 \ 4, 4))
    assert(items(46) === Event.Long(Root \ 1 \ 4 \ 5, 5))
    assert(items(47) === Event.Long(Root \ 2 \ 0, 0))
    assert(items(48) === Event.Long(Root \ 2 \ 1, 1))
    assert(items(49) === Event.Long(Root \ 2 \ 2, 2))
    assert(items(50) === Event.Long(Root \ 2 \ 3, 3))
    assert(items(51) === Event.Long(Root \ 2 \ 4, 4))
    assert(items(52) === Event.Long(Root \ 2 \ 5, 5))
    assert(items(53) === Event.End)
  }

  test("broken json") {
    val jsonSample = readTextFrom("/broken-json.json")
    val items = parseJson(jsonSample).toVector

    val Regex = "\\QUnexpected close marker '}': expected ']' (for ARRAY starting at.*".r

    assert(items(0) === Event.Long(Root \ "hello" \ 0 \ "world", 1))
    assert(items(1).isInstanceOf[Event.Error])
    assert(items(1).asInstanceOf[Event.Error].path === Root \ "hello" \ 0 \ "alex" \ 0)
    assert(!Regex.pattern.matcher(items(1).asInstanceOf[Event.Error].error).matches())
  }

  test("empty json") {
    val jsonSample = readTextFrom("/empty-test.json")
    parseJson(jsonSample).toList == List(Event.End)
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
