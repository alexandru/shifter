package shifter.validations.requests

trait MapValidation[T] extends MPValidation[T] {
  def apply(key: String, value: MultiParams[T]): MPValidated

  def apply(value: (String, MultiParams[T])): MPValidated =
    apply(value._1, value._2)
}
