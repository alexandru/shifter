package shifter.validations

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import shifter.validations.base._
import shifter.validations.requests._


@RunWith(classOf[JUnitRunner])
class ValidationDSLSuite extends FunSuite {
  abstract class Result(body: String)
  case object IsSuccess extends Result("success")
  case class IsFailure(body: String) extends Result(body)

  val params: Map[String, Seq[String]] = Map(
    "age" -> Seq("30"),
    "name" -> Seq("Alexandru"),
    "phones" -> Seq("0721254943", "021254943"),
    "zipcode" -> Seq("90210")
  )

  test("simple successful validation") {
    val result = // continued ...

    validate(params).where(
      "age"  is required & integer,
      "name" is required
    )
    .ifValid {
      IsSuccess
    }
    .orElse { e =>
      IsFailure(e.toString)
    }

    assert(result === IsSuccess)
  }  

  test("simple failure validation") {
    val invalidParams = params + ("age" -> Seq("aaa"))
    val result = // continued ...

    validate(invalidParams).where(
      "age"  is required & integer,
      "name" is required
    )
    .ifValid {
      IsSuccess
    }
    .orElse { e =>
      IsFailure(e("age"))
    }
    
    assert(result === IsFailure("must be an integer"))
  }  
}
