package shifter.validations

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ValidationSuite extends FunSuite {
  val params: Map[String, Set[String]] = Map(
    "age" -> Set("30"),
    "name" -> Set("Alexandru"),
    "phones" -> Set("0721254943", "021254943"),
    "zipcode" -> Set("90210")
  )

  test("simple required-param validation") {
    var beenInSuccessBranch = false
    val result = validate (params) (
      "name" -> required,
      "age" -> required
    ) onFailure { e =>      
      assert(false, "should not be a failure")
    } onSuccess {
      beenInSuccessBranch = true
    }

    assert(beenInSuccessBranch, "onSuccess wasn't executed")
    assert(result === Success())
  }

  test("required-params fails") {
    validate (Map.empty[String, Set[String]]) (
      "name" -> required,
      "age" -> required
    ) onFailure {
      (errors) =>
	assert(errors === Map("name" -> "is required", "age" -> "is required"))
    } onSuccess {
      assert(false, "should not have been a success")
    }
  }  
}
