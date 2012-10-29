package shifter.validations

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ValidatedSuite extends FunSuite {
  test("success & error & error & success") {
    val error1 = Failure("some error")
    val error2 = Failure("some other error")
    val success1 = Success
    val success2 = Success

    val result = success1 & error1 & error2 & success2
    assert(result === Failure("some error"))

    val result2 = success1 | success2
    assert(result2 === Success)
  }

  test("error | success | error | success") {
    val error1 = Failure("some error")
    val error2 = Failure("some other error")
    val success = Success

    val result1 = error1 | error2 | success | error1
    assert(result1 === Success)

    val result2 = error1 | error2 
    assert(result2 === error2)
  }

  test("Seqs of errors compose") {
    val error1 = failure("some error")
    val error2 = failure("some other error")

    val result = success & error1 & success & error2 & success
    assert(result === failure("some error", "some other error"))
  }

  test("Sets of errors compose") {
    val error1 = Failure(Set("some error"))
    val error2 = Failure(Set("some other error"))

    val result = success & error1 & success & error2 & success
    assert(result === failure("some error", "some other error"))
  }

  test("Maps of errors compose") {
    val error1 = failure("name" -> "not valid")
    val error2 = failure("age" -> "must be over 30")
    val error3 = failure("age" -> "must be over 18")

    val result1 = success & error1 & success & error2 & success
    assert(result1 === failure("name" -> "not valid", "age" -> "must be over 30"))

    val result2 = error2 & error3
    assert(result2 === failure("age" -> "must be over 30"))
    val result3 = error3 & error2
    assert(result3 === failure("age" -> "must be over 18"))

    // working with a mutable map should work
    val errorSpecial = Failure(collection.mutable.Map("zipcode" -> "is missing"))
    val resultSpecial = error1 & errorSpecial
    assert(resultSpecial === failure("name" -> "not valid", "zipcode" -> "is missing"))
  }
}
