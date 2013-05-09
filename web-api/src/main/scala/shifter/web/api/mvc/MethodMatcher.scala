package shifter.web.api.mvc

import shifter.web.api.requests.RequestHeader
import shifter.web.api.http.{HttpMethod => m}

class MethodMatcher(method: Option[m.Value]) {
  def unapply(req: RequestHeader) =
    if (method.isEmpty || req.method == method.get)
      Some(req.path)
    else
      None
}

object ALL  extends MethodMatcher(None)
object GET extends MethodMatcher(Some(m.GET))
object HEAD extends MethodMatcher(Some(m.HEAD))
object OPTIONS extends MethodMatcher(Some(m.OPTIONS))
object POST extends MethodMatcher(Some(m.POST))
object PUT extends MethodMatcher(Some(m.PUT))



