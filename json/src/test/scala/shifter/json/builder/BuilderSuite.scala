package shifter.json.builder

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.io.{InputStreamReader, BufferedReader}

@RunWith(classOf[JUnitRunner])
class BuilderSuite extends FunSuite {
  test("empty objects") {
    assert(Json.obj().prettyPrint === "{}")
    assert(Json.obj().compactPrint === "{}")
    assert(Json.arr().prettyPrint === "[]")
    assert(Json.arr().compactPrint === "[]")
    assert(Json.arr(Json.arr()).compactPrint === "[[]]")
    assert(Json.arr(Json.arr(), Json.arr()).compactPrint === "[[],[]]")
    assert(Json.arr(Json.arr(), Json.arr(JsNull)).compactPrint === "[[],[null]]")
    assert(Json.arr(Json.obj()).compactPrint === "[{}]")
    assert(Json.arr(Json.obj(), Json.obj()).compactPrint === "[{},{}]")
    assert(Json.arr(Json.obj(), Json.obj()).prettyPrint === "[\n    {},\n    {}\n]")
    assert(Json.obj("hello" -> Json.arr()).compactPrint === """{"hello":[]}""")
  }

  test("json string escape") {
    assert(Json.str("hello ' world").compactPrint === "\"hello ' world\"")
    assert(Json.str("hello \" world").compactPrint === "\"hello \\\" world\"")
  }

  test("generate json") {
    val simpleObj = Json.obj(
      "string-sample" -> Json.str("Hello \"world\"!"),
      "null-string-sample" -> Json.str(None),
      "some-string-sample" -> Json.str(Some("Another String!")),

      "double-sample" -> Json.double(12.12),
      "null-double-sample" -> Json.double(None),
      "some-double-sample" -> Json.double(Some(29.2913)),

      "long-sample" -> Json.long(Long.MaxValue),
      "null-long-sample" -> Json.long(None),
      "some-long-sample" -> Json.long(Some(Long.MinValue)),

      "int-sample" -> Json.int(Int.MaxValue),
      "null-int-sample" -> Json.int(None),
      "some-int-sample" -> Json.int(Some(Int.MinValue)),

      "array-sample" -> Json.arr(1, 2, 3),

      "bool-sample" -> Json.bool(true),
      "null-bool-sample" -> Json.bool(None),
      "some-bool-sample" -> Json.bool(Some(false)),

      "null-sample" -> JsNull
    )

    val complexObj = Json.obj(
      "array" -> Json.arr(simpleObj, simpleObj),
      "obj-in-array" -> Json.arr(Json.obj(
        "my-array" -> Json.arr(simpleObj, simpleObj)
      )),
      "array-in-array" -> Json.arr(
        Json.arr(Json.int(1), Json.int(2), Json.arr(3), Json.int(4), Json.arr(5)),
        Json.arr(Json.arr(1), Json.int(2), Json.arr(3), Json.int(4), Json.arr(5))
      )
    )

    val prettyTest = readTextFrom("/builder-pretty-test.json")
    assert(complexObj.prettyPrint === prettyTest.trim)

    val compactTest = readTextFrom("/builder-compact-test.json")
    assert(complexObj.compactPrint === compactTest.trim)
  }

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