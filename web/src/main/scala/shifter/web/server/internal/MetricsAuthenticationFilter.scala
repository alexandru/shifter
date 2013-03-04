package shifter.web.server.internal

import shifter.web.api._
import shifter.web.api.Request
import shifter.web.server.MetricsConfiguration


class MetricsAuthenticationFilter(config: MetricsConfiguration)
    extends BlockingUrlsHandler with BasicAuthSupport {

  def routes: PartialFunction[Request, Response] = {
    case req if isForMetrics(req.path) && !isAuthenticated(req, config.username, config.password) =>
      HttpUnauthenticated(config.realm)
  }

  private[this] def isForMetrics(path: String) =
    metricsRegex.findFirstIn(path).isDefined

  private[this] val metricsRegex =
    ("^" + config.mapping.replaceAll("[*]", ".*") + "$").r
}
