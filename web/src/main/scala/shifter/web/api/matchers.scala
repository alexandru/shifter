package shifter.web.api

import shifter.web.api.{HttpMethod => m}

abstract class PathMatcher[T, U <: HttpRequest[T]](isMethodValid: Set[HttpMethod.Value], parser: RequestParser[T, U]) {
  def builder(raw: HttpRawRequest): HttpLazyRequest[T, U] =
    new HttpLazyRequest(raw, parser)

  def unapply(raw: HttpRawRequest) =
    if (isMethodValid(raw.method) && parser.canBeParsed(raw)) {
      val segments = raw.path.split("/").filterNot(_.isEmpty).toList
      Some((segments, builder(raw)))
    }
    else
      None
}

object PATH {
  def unapply(req: HttpRawRequest) = {
    val segments = req.path.split("/").filterNot(_.isEmpty).toList
    Some(segments)
  }
}

object GET extends PathMatcher(Set(m.GET), HttpRequestWithoutBody)
object HEAD extends PathMatcher(Set(m.HEAD), HttpRequestWithoutBody)
object OPTIONS extends PathMatcher(Set(m.OPTIONS), HttpRequestWithoutBody)

object POST extends PathMatcher(Set(m.POST), HttpFormRequest)
object POSTForm extends PathMatcher(Set(m.POST), HttpFormRequest)
object POSTMultiPart extends PathMatcher(Set(m.POST), HttpMultiPartFormRequest)
object POSTJson extends PathMatcher(Set(m.POST), HttpJsonRequest)

object ALL  extends PathMatcher(HttpMixedFormRequest.validMethods, HttpRequestWithoutBody)
object FORM extends PathMatcher(HttpMixedFormRequest.validMethods, HttpMixedFormRequest)

object PUT extends PathMatcher(Set(m.PUT), HttpFormRequest)
object PUTForm extends PathMatcher(Set(m.PUT), HttpFormRequest)
object PUTMultiPart extends PathMatcher(Set(m.PUT), HttpMultiPartFormRequest)
object PUTJson extends PathMatcher(Set(m.PUT), HttpJsonRequest)

