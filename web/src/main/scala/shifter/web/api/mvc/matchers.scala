package shifter.web.api.mvc

import shifter.web.api.base.{HttpMethod => m}
import shifter.web.api.requests._

class PathMatcher(method: Option[m.Value]) {
  def unapply(req: HttpRequest[_]) =
    if (method.isEmpty || req.method == method.get)
      Some(req.path)
    else
      None
}

object Path {
  def unapply(path: String) = {
    val segments = path.split("/").filterNot(_.isEmpty).toList
    Some(segments)
  }
}

object ALL  extends PathMatcher(None)
object GET extends PathMatcher(Some(m.GET))
object HEAD extends PathMatcher(Some(m.HEAD))
object OPTIONS extends PathMatcher(Some(m.OPTIONS))
object POST extends PathMatcher(Some(m.POST))
object PUT extends PathMatcher(Some(m.PUT))



