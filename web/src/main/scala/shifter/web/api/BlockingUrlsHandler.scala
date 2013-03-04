package shifter.web.api

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}


trait BlockingUrlsHandler extends UrlsHandler {

  def routes: PartialFunction[Request, Response]

  final def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    val req = Request(request.asInstanceOf[HttpServletRequest])
    val resp = response.asInstanceOf[HttpServletResponse]

    if (!applicableFor(req) || !routes.isDefinedAt(req)) {
      chain.doFilter(request, response)
      return
    }

    onRequestEvent(req)
    try {
      val value = routes(req)
      writeResponse(request, resp, chain, req, value)
    }
    catch {
      case ex: Throwable =>
        logger.error("Couldn't finish processing the request", ex)

        writeResponse(request, resp, chain, req, HttpError(
          status = 500,
          body = "500 Internal Server Error",
          contentType = "text/plain"
        ))

        if (ex.isInstanceOf[Error] || ex.getCause.isInstanceOf[Error]) {
          logger.error("Last error was fatal, shutting down server")
          Runtime.getRuntime.exit(1000)
        }
    }
  }
}
