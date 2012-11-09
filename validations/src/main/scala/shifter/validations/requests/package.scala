package shifter.validations

import collection.GenTraversable
import shifter.validations.base._

package object requests {
  type MultiParams[T]  = Map[String, GenTraversable[T]]  
  type MPValidated     = Validated[Map[String, String]]
  type MPValidation[T] = Validation[(String, MultiParams[T]), Map[String, String]]

  def success() = 
    Success

  object failure {
    def apply[T](head: (String, T), tail: (String, T)*) =
      Failure(Map(head) ++ tail.toMap)
  
    def apply(head: String, tail: String*) =
      Failure(head +: tail.toSeq)
  }

  implicit def ApplyStringExtension(string: String) =
    new StringExtension(string)
}
