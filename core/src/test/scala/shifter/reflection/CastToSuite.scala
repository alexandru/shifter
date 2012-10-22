package shifter.reflection

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CastToSuite extends FunSuite {
  test("string representing number casts to int") {
    assert(castTo[Int]("1") === Some(1))
    assert(castTo[Long]("1") === Some(1L))
    assert(castTo[Char]("1") === Some('1'))
    assert(castTo[Char]("11") === Some('1'))
    assert(castTo[Byte]("1") === Some(1.toByte))
    assert(castTo[Short]("1") === Some(1.toShort))
    assert(castTo[Boolean]("1") === Some(true))
    assert(castTo[Boolean]("0") === Some(false))
  }

  test("string representing number, when casting to int is None") {
    assert(castTo[Int]("aaa") === None)
    assert(castTo[Long]("aaa") === None)
    assert(castTo[Char]("aaa") === None)
    assert(castTo[Byte]("aaa") === None)
    assert(castTo[Short]("aaa") === None)
    assert(castTo[Boolean]("aaa") === None)
    assert(castTo[Boolean]("aaa") === None)
  }
}
