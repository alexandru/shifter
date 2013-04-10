package shifter.web.api

import language.existentials
import shifter.web.api.requests.RawRequest
import shifter.web.api.responses.HttpResponse

package object mvc {
  type ActionResponse = RawRequest => HttpResponse[_]

  type UrlRoutes = PartialFunction[RawRequest, ActionResponse]
}
