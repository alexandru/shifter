package shifter

import collection.GenTraversable


package object validations {
  type MultiParams[T]  = Map[String, GenTraversable[T]]  
  type MPValidated     = Validated[Map[String, String]]
  type MPValidation[T] = Validation[(String, MultiParams[T]), Map[String, String]]

  trait MapValidation[T] extends MPValidation[T] {
    def apply(key: String, value: MultiParams[T]): MPValidated

    def apply(value: (String, MultiParams[T])): MPValidated =
      apply(value._1, value._2)
  }

  class RequiredField[T] extends MapValidation[T] {
    def apply(key: String, value: MultiParams[T]): MPValidated = 
      Success
  }

  def success() = Success

  object failure {
    def apply[T](head: (String, T), tail: (String, T)*) =
      Failure(Map(head) ++ tail.toMap)

    def apply(head: String, tail: String*) =
      Failure(head +: tail.toSeq)
  }


  class StringHelpers(string: String) {
    def is(validation: MPValidation[String]) =
      (string, validation)
  }

  implicit def StringToHelper(string: String) =
    new StringHelpers(string)

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
    def apply(value: (String, MultiParams[String])) = {
      val (key, map) = value
      map.get(key) match {
	case Some(Number) => Success
	case None => Success
	case _ => failure(key -> "must be an integer")
      }
    }    

    private[this] val Number = """^\d+$""".r
  }

  def validate(params: MultiParams[String])(validations: (String, MPValidation[String])*): MPValidated =
    validations.foldLeft(Success.asInstanceOf[MPValidated]) {
      (acc, elem) => val (key, validation) = elem
      acc & validation(key -> params)
    }
}
