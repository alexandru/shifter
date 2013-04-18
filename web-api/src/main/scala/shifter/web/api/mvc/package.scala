package shifter.web.api

import language.existentials
import shifter.web.api.requests.RawRequest
import shifter.web.api.responses.HttpResponse

package object mvc {
  type Action = RawRequest => HttpResponse[_]

  type UrlRoutes = PartialFunction[RawRequest, Action]

  implicit class ImplicitPathMatcher(val sc: StringContext) extends AnyVal {
    def path = new PathMatcher.Path(sc.parts)
    def p = new PathMatcher.Path(sc.parts)
  }
}
