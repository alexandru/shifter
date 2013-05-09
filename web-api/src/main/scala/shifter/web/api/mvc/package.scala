package shifter.web.api

import language.existentials
import shifter.web.api.requests.RequestHeader

package object mvc {
  type UrlRoutes = PartialFunction[RequestHeader, Action[_]]

  implicit class ImplicitPathMatcher(val sc: StringContext) extends AnyVal {
    def path = new PathMatcher.Path(sc.parts)
    def p = new PathMatcher.Path(sc.parts)
  }

}
