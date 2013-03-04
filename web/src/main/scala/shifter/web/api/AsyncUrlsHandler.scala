package shifter.web.api

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import concurrent.{ExecutionContext, Future}
import util.{Failure, Success}
import com.yammer.metrics.Metrics
import java.util.concurrent.TimeUnit


trait AsyncUrlsHandler extends UrlsHandler {
  implicit def context: ExecutionContext

  def routes: PartialFunction[Request, Future[Response]]

  protected def requestTimeout: Int = 10000

  protected def triggeredTimeoutResponse(request: Request): Response =
    HttpRequestTimeout("408 Timeout", "text/plain")

  protected def onRequestTimeout(request: Request) {
    timeouts.map(_.mark())
  }

  final def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    val req = Request(request.asInstanceOf[HttpServletRequest])
    val resp = response.asInstanceOf[HttpServletResponse]

    if (!applicableFor(req) || !routes.isDefinedAt(req)) {
      chain.doFilter(request, response)
      return
    }

    onRequestEvent(req)
    val future: Future[Response] = routes(req)

    if (future.isCompleted) {
      future.value.get match {
        case Success(value) =>
          writeResponse(request, resp, chain, req, value)

        case Failure(ex) =>
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
    else {
      val ctx = req.underlying.startAsync(request, response)
      val committed = Array(false)

      ctx.setTimeout(requestTimeout)

      ctx.addListener(new AsyncListener {
        def onError(event: AsyncEvent) {}

        def onComplete(event: AsyncEvent) {}

        def onStartAsync(event: AsyncEvent) {}

        def onTimeout(event: AsyncEvent) {
          committed.synchronized {
            if (!committed(0)) {
              committed(0) = true
              writeResponse(request, resp, chain, req, triggeredTimeoutResponse(req))
              ctx.complete()
            }
          }
        }
      })

      // dealing with failures (exceptions thrown)
      future.onFailure {
        case ex =>
          committed.synchronized {
            if (!committed(0)) {
              committed(0) = true

              writeResponse(request, resp, chain, req, HttpError(
                status = 500,
                body = "500 Internal Server Error",
                contentType = "text/plain"
              ))

              ctx.complete()
              logger.error("Couldn't finish processing the request", ex)
            }
          }

          if (ex.isInstanceOf[Error] || ex.getCause.isInstanceOf[Error]) {
            logger.error("Last error was fatal, shutting down server")
            Runtime.getRuntime.exit(1000)
          }
      }

      future.onSuccess {
        case value =>
          committed.synchronized {
            if (!committed(0)) {
              committed(0) = true
              writeResponse(request, resp, chain, req, value)
              ctx.complete()
            }
          }
      }
    }
  }

  private[this] val timeouts =
    if (httpConfig.isInstrumented)
      Some(Metrics.newMeter(this.getClass, "timeouts", "requests", TimeUnit.SECONDS))
    else
      None
}
