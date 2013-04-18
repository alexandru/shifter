package shifter.web.server.internal

/*

import shifter.web.server.MetricsConfiguration
import javax.servlet
import javax.servlet.{FilterConfig, FilterChain, ServletResponse, ServletRequest}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import shifter.web.api.utils.BasicAuthSupport


class MetricsAuthenticationFilter(config: MetricsConfiguration) extends servlet.Filter with BasicAuthSupport {
  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    val httpRequest = request.asInstanceOf[HttpServletRequest]
    val httpResponse = response.asInstanceOf[HttpServletResponse]

    val servletPath = Option(httpRequest.getServletPath)
    val pathInfo = Option(httpRequest.getPathInfo)
    val path = Seq(servletPath, pathInfo).flatten.foldLeft("")(_+_)

    if (isForMetrics(path)) {
      val auth = httpRequest.getHeader("Authorization")
      if (!isAuthenticated(auth, config.username, config.password)) {
        httpResponse.setStatus(401)
        httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"%s\"".format(config.realm))
        httpResponse.setContentType("text/plain")

        val body = "Unauthenticated".getBytes("UTF-8")
        httpResponse.setContentLength(body.length)

        val writer = httpResponse.getOutputStream
        writer.write(body)
        writer.close()
      }
      else
        chain.doFilter(request, response)
    }
    else
      chain.doFilter(request, response)
  }

  private[this] def isForMetrics(path: String) =
    metricsRegex.findFirstIn(path).isDefined

  private[this] val metricsRegex =
    ("^" + config.mapping.replaceAll("[*]", ".*") + "$").r

  def init(filterConfig: FilterConfig) {}
  def destroy() {}
}

*/
