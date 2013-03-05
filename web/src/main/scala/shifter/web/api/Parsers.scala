package shifter.web.api

trait Parsers extends Any {
  def raw: HttpRawRequest

  def isWithoutBody = HttpRequestWithoutBody.canBeParsed(raw)

  def asWithoutBody = HttpRequestWithoutBody.parse(raw)

  def isForm = HttpFormRequest.canBeParsed(raw)

  def asForm = HttpFormRequest.parse(raw)

  def isMultiPart = HttpMultiPartFormRequest.canBeParsed(raw)

  def asMultiPart = HttpMultiPartFormRequest.parse(raw)

  def isJson = HttpJsonRequest.canBeParsed(raw)

  def asJson = HttpJsonRequest.parse(raw)

  def asMixed = HttpMixedFormRequest.parse(raw)

}

