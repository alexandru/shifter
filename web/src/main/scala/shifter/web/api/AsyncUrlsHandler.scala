package shifter.web.api

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import concurrent.{ExecutionContext, Future}
import util.{Failure, Success}
import com.yammer.metrics.Metrics
import java.util.concurrent.TimeUnit


trait AsyncUrlsHandler extends UrlsHandler {
  implicit def context: ExecutionContext

  def routes: PartialFunction[HttpRawRequest, Future[HttpResponse[_]]]

  protected def requestTimeout: Int = 10000

  protected def triggeredTimeoutResponse(request: HttpRequest[_]): HttpResponse[_] =
    HttpRequestTimeout("408 Timeout").withHeader("Content-Type" -> "text/plain")

  protected def onRequestTimeout(request: HttpRequest[_]) {
    timeouts.map(_.mark())
  }

  final def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    val ourRequest = new HttpRawRequest(request.asInstanceOf[HttpServletRequest])
    val resp = response.asInstanceOf[HttpServletResponse]

    if (!applicableFor(ourRequest) || !routes.isDefinedAt(ourRequest)) {
      chain.doFilter(request, response)
      return
    }

    onRequestEvent(ourRequest)
    val future: Future[HttpResponse[_]] = routes(ourRequest)

    if (future.isCompleted) {
      future.value.get match {
        case Success(value) =>
          writeResponse(request, resp, chain, ourRequest, value)

        case Failure(ex) =>
          logger.error("Couldn't finish processing the request", ex)

          writeResponse(request, resp, chain, ourRequest, HttpError(
            status = 500,
            body = "500 Internal Server Error"
          ).withHeader("Content-Type" -> "text/plain"))

          if (ex.isInstanceOf[Error] || ex.getCause.isInstanceOf[Error]) {
            logger.error("Last error was fatal, shutting down server")
            Runtime.getRuntime.exit(1000)
          }
      }
    }
    else {
      val ctx = ourRequest.underlying.startAsync(request, response)
      val committed = Array(x = false)

      ctx.setTimeout(requestTimeout)

      ctx.addListener(new AsyncListener {
        def onError(event: AsyncEvent) {}

        def onComplete(event: AsyncEvent) {}

        def onStartAsync(event: AsyncEvent) {}

        def onTimeout(event: AsyncEvent) {
          committed.synchronized {
            if (!committed(0)) {
              committed(0) = true
              writeResponse(request, resp, chain, ourRequest, triggeredTimeoutResponse(ourRequest))
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

              writeResponse(request, resp, chain, ourRequest, HttpError(
                status = 500,
                body = "500 Internal Server Error"
              ).addHeader("Content-Type", "text/plain"))

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
              writeResponse(request, resp, chain, ourRequest, value)
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
