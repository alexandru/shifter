package shifter

package validations {
  class MapKeyValidation(key: String, validation: MultiParamsValidation) extends MultiParamsValidationKey {
    def apply(value: (String, MultiParams)): MultiParamsValidatedKey = {
      val v = value._2.collect { case (k,v) if k == key => (k,v) }
      validation(v) match {
	case Failure(error) =>
	  Failure(Map(key -> error))
	case Success() =>
	  Success()
      }
    }
  }
}

package object validations {
  type MultiParams = Map[String, Set[String]]
  type MultiParamsValidation = Validation[MultiParams, String]
  type MultiParamsValidated = Validated[MultiParams, String]

  type MultiParamsValidatedKey = Validated[(String, MultiParams), Map[String, String]]
  type MultiParamsValidationKey = Validation[(String, MultiParams), Map[String, String]]
  
  def required = new MultiParamsValidation {
    def apply(values: MultiParams): MultiParamsValidated = 
      if (values.size > 0)
	Success()
      else
	Failure("is required")
  }

  def validate(params: MultiParams)(validations: (String, Validation[MultiParams, String])*) = {
    validations.map { 
      case (key, validation) => 
	new MapKeyValidation(key, validation)((key, params))
    } reduceLeft { (acc, elem) =>
      acc match {
	case Failure(errors1) =>
	  elem match {
	    case Failure(errors2) =>
	      Failure(errors1 ++ errors2)
	    case Success() =>
	      acc
	  }
	case Success() =>
	  elem
      }
    }
  }
}

