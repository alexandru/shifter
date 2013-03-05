package shifter.web.api

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}


trait BlockingUrlsHandler extends UrlsHandler {

  def routes: PartialFunction[HttpRawRequest, HttpResponse[_]]

  final def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    val rawRequest = new HttpRawRequest(request.asInstanceOf[HttpServletRequest])
    val servletResponse = response.asInstanceOf[HttpServletResponse]

    if (!applicableFor(rawRequest) || !routes.isDefinedAt(rawRequest)) {
      chain.doFilter(request, response)
      return
    }

    onRequestEvent(rawRequest)
    try {
      val ourResponse = routes(rawRequest)
      writeResponse(request, servletResponse, chain, rawRequest, ourResponse)
    }
    catch {
      case ex: Throwable =>
        logger.error("Couldn't finish processing the request", ex)

        writeResponse(request, servletResponse, chain, rawRequest, HttpError(
          status = 500,
          body = "500 Internal Server Error"
        ).withHeader("Content-Type" -> "text/plain"))

        if (ex.isInstanceOf[Error] || ex.getCause.isInstanceOf[Error]) {
          logger.error("Last error was fatal, shutting down server")
          Runtime.getRuntime.exit(1000)
        }
    }
  }
}
