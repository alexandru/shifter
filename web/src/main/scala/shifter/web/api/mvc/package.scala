package shifter.web.api

import shifter.web.api.requests.RawRequest
import shifter.web.api.responses.HttpResponse

package object mvc {
  type ActionResponse[T] = RawRequest => HttpResponse[T]

  type UrlRoutes = PartialFunction[RawRequest, ActionResponse[_]]
}
