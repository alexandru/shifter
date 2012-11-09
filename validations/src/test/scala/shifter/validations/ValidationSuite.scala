package shifter.validations

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class ValidationSuite extends FunSuite {
  import shifter.validations.base._
  import shifter.validations.requests._

  def createSimpleCheck(test: String) = 
    Validation {
      (value: String) => if (value == test)
	Success
      else
	Failure(Set(test))
    }

  def createMapCheck(key: String) =
    Validation {
      (value: (String, String)) => if (value._1 == key)
	Success
      else
	failure(key -> "is missing")
    }

  test("simple validations compose") {
    val validation1 = createSimpleCheck("hello")
    val validation2 = createSimpleCheck("world")
    val validation3 = createSimpleCheck("alex")

    val dualValidation = validation1 & validation2
    assert(dualValidation("hello") === Failure(Seq("world")))
    assert(dualValidation("world") === Failure(Seq("hello")))
    assert(dualValidation("alex")  === Failure(Seq("hello", "world")))

    val fallbackValidation1 = validation1 & validation2 | validation3
    assert(fallbackValidation1("alex")  === Success)
    val fallbackValidation2 = validation3 | validation1 & validation2
    assert(fallbackValidation2("alex")  === Success)

    val all3 = validation1 & validation2 & validation3
    assert(all3("alex") === Failure(Seq("hello", "world")))
    assert(all3("aaa")  === Failure(Seq("hello", "world", "alex")))
  }

  test("Maps validations compose") {
    val requireName = createMapCheck("name")
    val requireAddr = createMapCheck("address")

    val dual = requireName & requireAddr
    assert(dual("name"-> "alex") == failure("address" -> "is missing"))
    assert(dual("zipcode"-> "90210") == failure("name" -> "is missing", "address" -> "is missing"))
  }
}
