package shifter.web.api


object Path {
  def unapply(req: Request) = {
    val segments = req.path.split("/").filterNot(_.isEmpty).toList
    Some(segments)
  }
}

abstract class PathMatcher(method: String) {
  def unapply(req: Request) =
    if (req.method == method) {
      val segments = req.path.split("/").filterNot(_.isEmpty).toList
      Some(segments)
    }
    else
      None
}

object GET extends PathMatcher("GET")

object POST extends PathMatcher("POST")

object PUT extends PathMatcher("PUT")

object DELETE extends PathMatcher("DELETE")

object OPTIONS extends PathMatcher("OPTIONS")