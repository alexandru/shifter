package shifter.validations.requests

import shifter.validations.base._

class WorkflowException(msg: String) extends RuntimeException

class StartValidate(params: MultiParams[String]) {
  def where(validations: (String, MPValidation[String])*) = {
    val validated = validations.foldLeft(Success.asInstanceOf[MPValidated]) {
      (acc, elem) => val (key, validation) = elem
      acc & validation(key -> params)
    }
    new IfValid(validated)
  }
}

class IfValid(validated: MPValidated) {
  def ifValid[R](f: => R) = 
    validated match {
      case Success => new IfError(validated, Some(f))
      case _ => new IfError(validated, None)
    }
}

class IfError[T](validated: MPValidated, success: Option[T]) {
  def orElse[R >: T](f: Map[String, String] => R): R =
    validated match {
      case Success => success.get
      case Failure(error) => f(error)
    }
}

object validate {
  def apply(params: MultiParams[String]) =
    new StartValidate(params)
}
