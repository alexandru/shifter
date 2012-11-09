package shifter.validations.requests


class StringExtension(string: String) {
  def is(validation: MPValidation[String]) =
    (string, validation)
}
