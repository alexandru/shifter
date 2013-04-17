package shifter.utils

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GlobSuite extends FunSuite {
  test("simple decompose") {
    val result = glob.decompose("/hello/*/world", "/hello/alex/world")
    assert(result === List("/hello/", "alex", "/world"))
  }

  test("complex decompose") {
    val result = glob.decompose("a*b*c*b*a", "aaabababbcccbbbaa")
    assert(result.mkString === "aaabababbcccbbbaa")
    assert(result.length === 9)
    assert(result == List("a", "aababab", "b", "cc", "c", "bb", "b", "a", "a"))
  }

  test("no wildcard") {
    val result = glob.decompose("no wildcard", "no wildcard")
    assert(result === List("no wildcard"))
  }

  test("only one wildcard") {
    val result = glob.decompose("*", "aaabababbcccbbbaa")
    assert(result === List("aaabababbcccbbbaa"))
  }

  test("wildcard prefix") {
    val result = glob.decompose("a*", "aaabababbcccbbbaa")
    assert(result === List("a", "aabababbcccbbbaa"))
  }

  test("wildcard suffix") {
    val result = glob.decompose("*a", "aaabababbcccbbbaa")
    assert(result === List("aaabababbcccbbba", "a"))
  }

  test("wildcard prefix and suffix") {
    val result = glob.decompose("*a*", "aaabababbcccbbbaa")
    assert(result === List("aaabababbcccbbb", "a", "a"))
  }

  test("decompose failure 1") {
    val result = glob.decompose("aaa*b", "aaacccdddeeefff")
    assert(result === Nil)
  }

  test("decompose failure 2") {
    val result = glob.decompose("aaa*bbb", "aaabbb")
    assert(result === Nil)
  }

  test("decompose empty string 1") {
    val result = glob.decompose("aaa*b", "")
    assert(result === Nil)
  }

  test("decompose empty string 2") {
    val result = glob.decompose("*", "")
    assert(result === Nil)
  }

  test("decompose empty string 3") {
    val result2 = glob.decompose("**", "")
    assert(result2 === Nil)
  }

  test("decompose empty string 4") {
    val result = glob.decompose("*a*", "")
    assert(result === Nil)
  }

  test("decompose empty string 5") {
    val result = glob.decompose("*a", "")
    assert(result === Nil)
  }

  test("decompose empty string 6") {
    val result = glob.decompose("a*", "")
    assert(result === Nil)
  }

  test("decompose empty pattern") {
    try {
      glob.decompose("", "")
      assert(condition = false)
    }
    catch {
      case _: IllegalArgumentException =>
        assert(condition = true)
    }
  }

  test("decompose large string") {
    val str = "a" * 1000 + "b" * 1000
    val result = glob.decompose("a*b*", str)

    assert(result.length === 4)
    assert(result.mkString === str)

    val result2 = glob.decompose(str, str)

    assert(result2.length === 1)
    assert(result2.mkString === str)
  }

  test("wildcard does not match blank") {
    val result = glob.decompose("aaaa*bbbb", "aaaabbbb")
    assert(result === Nil)

    val result2 = glob.decompose("aaaa*", "aaaa")
    assert(result2 === Nil)

    val result3 = glob.decompose("*aaaa", "aaaa")
    assert(result3 === Nil)
  }

}
