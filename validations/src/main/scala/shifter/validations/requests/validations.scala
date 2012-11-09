package shifter.validations.requests

import shifter.validations.base._

object required extends MPValidation[String] {
  def apply(value: (String, MultiParams[String])) = {
    val (key, map) = value
    if (map.contains(key))
      Success
    else
      failure(key -> "is required")
  }
}

object integer extends MPValidation[String] {
  def apply(elem: (String, MultiParams[String])) = {
    val (key, map) = elem
    val isValid = map.get(key).getOrElse(Nil).forall {
      case Number() => true
      case x => false
    }

    if (isValid) Success
    else failure(key -> "must be an integer")
  }    

  private[this] val Number = """^\d+$""".r
}

