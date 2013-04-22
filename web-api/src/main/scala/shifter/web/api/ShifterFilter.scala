package shifter.web.api

import language.existentials
import shifter.web.api.mvc._
import javax.servlet.{Filter => JavaFilter, _}
import shifter.web.api.requests.RawRequest
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal
import com.typesafe.scalalogging.slf4j.Logging
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import shifter.web.api.responses._
import shifter.web.api.responses.StreamResponse
import shifter.web.api.responses.SimpleResponse
import shifter.web.api.responses.AsyncResponse
import shifter.web.api.responses.BytesResponse
import shifter.web.api.http.HeaderNames._
import scala.annotation.tailrec
import shifter.concurrency.atomic.Ref
import scala.util.{Success, Failure}


trait ShifterFilter extends JavaFilter with ResponseBuilders with Logging {
  def routes: UrlRoutes
  implicit def ec: ExecutionContext

  final def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, chain: FilterChain) {
    val rawRequest = new RawRequest(servletRequest.asInstanceOf[HttpServletRequest])

    val action =
      try
        routes(rawRequest)
      catch {
        case _: MatchError =>
          null
      }

    if (action != null)
      handleAction(servletRequest, servletResponse, chain, rawRequest, action)
    else
      chain.doFilter(servletRequest, servletResponse)
  }

  @tailrec
  final def handleAction(servletRequest: ServletRequest, servletResponse: ServletResponse, chain: FilterChain, rawRequest: RawRequest, action: Action) {
    val continuation: Action = try
      action(rawRequest) match {
        case resp: CompleteResponse[_] =>
          processBlocking(
            servletRequest.asInstanceOf[HttpServletRequest],
            servletResponse.asInstanceOf[HttpServletResponse],
            chain,
            rawRequest,
            resp
          )
          null

        case AsyncResponse(future, timeout, timeoutResponse) =>
          processAsync(
            servletRequest.asInstanceOf[HttpServletRequest],
            servletResponse.asInstanceOf[HttpServletResponse],
            chain,
            rawRequest,
            future,
            timeout,
            timeoutResponse
          )
          null

        case ForwardResponse(other) =>
          other

        case ChainResponse =>
          chain.doFilter(servletRequest, servletResponse)
          null
      }
    catch {
      case NonFatal(ex) =>
        logger.error("Couldn't finish processing the request", ex)

        writeResponse(servletRequest, servletResponse.asInstanceOf[HttpServletResponse], chain, rawRequest, HttpError(
          status = 500,
          body = "500 Internal Server Error"
        ).withHeader("Content-Type" -> "text/plain"))

        null
    }

    if (continuation != null)
      handleAction(servletRequest, servletResponse, chain, rawRequest, continuation)
  }

  final def processAsync(
      servletRequest: HttpServletRequest, servletResponse: HttpServletResponse,
      chain: FilterChain, request: RawRequest, future: Future[CompleteResponse[_]],
      timeout: Duration, timeoutResponse: CompleteResponse[_]) {

    val ctx = servletRequest.startAsync(servletRequest, servletResponse)
    val committedRef = Ref(initialValue = false)
    val timeoutRef = Ref(initialValue = false)

    if (timeout.isFinite())
      ctx.setTimeout(timeout.toMillis)

    ctx.addListener(new AsyncListener {
      def onError(event: AsyncEvent) {}
      def onStartAsync(event: AsyncEvent) {}

      def onComplete(event: AsyncEvent) {
        if (future.value.isDefined)
          future.value.get match {
            case Failure(ex) =>
              writeResponse(servletRequest, servletResponse, chain, request, HttpError(
                status = 500,
                body = "500 Internal Server Error"
              ).addHeader("Content-Type", "text/plain"))

              logger.error("Couldn't finish processing the request", ex)

            case Success(response) =>
              writeResponse(servletRequest, servletResponse, chain, request, response)
          }
      }

      def onTimeout(event: AsyncEvent) {
        timeoutRef.set(true)
        val canCommit = committedRef.compareAndSet(expect = false, update = true)

        if (canCommit) {
          writeResponse(
            servletRequest,
            servletResponse,
            chain,
            request,
            timeoutResponse
          )

          ctx.complete()
        }
      }
    })

    // dealing with failures (exceptions thrown)
    future.onComplete { case _ =>
      val canCommit = !timeoutRef.get && committedRef.compareAndSet(expect = false, update = true)
      if (canCommit)
        try
          ctx.complete()
        catch {
          case ex: IllegalStateException if ex.getMessage.contains("expired") =>
            // nothing
        }
    }
  }

  final def processBlocking(
      servletRequest: HttpServletRequest, servletResponse: HttpServletResponse,
      chain: FilterChain, request: RawRequest, response: CompleteResponse[_]) {

    writeResponse(servletRequest, servletResponse, chain, request, response)
  }

  final def writeHeaders(request: RawRequest, response: CompleteResponse[_], servletResponse: HttpServletResponse) {
    for ((key, list) <- response.headers; value <- list)
      servletResponse.setHeader(key, value)

    if (!response.hasHeader("Cache-Control"))
      servletResponse.setHeader("Cache-Control", "must-revalidate,no-cache,no-store")

    if (!response.hasHeader(CONNECTION))
      if (request.protocol == "HTTP/1.1")
        servletResponse.setHeader(CONNECTION, request.header(CONNECTION).getOrElse("keep-alive"))
      else
        servletResponse.setHeader(CONNECTION, request.header(CONNECTION).getOrElse("close"))
  }

  final def writeResponse(
    servletRequest: ServletRequest, servletResponse: HttpServletResponse, chain: FilterChain,
    request: RawRequest, response: CompleteResponse[_]) {

    if (!servletResponse.isCommitted)
      response match {
        case SimpleResponse(status, headers, body) =>
          servletResponse.setStatus(status)
          servletResponse.setContentType(response.contentType)
          servletResponse.setCharacterEncoding("UTF-8")

          writeHeaders(request, response, servletResponse)

          val bytes = body.getBytes("UTF-8")
          servletResponse.setContentLength(bytes.length)

          val out = servletResponse.getOutputStream
          if (bytes.length > 0)
            out.write(bytes)
          out.close()

        case BytesResponse(status, headers, body) =>
          servletResponse.setStatus(status)
          servletResponse.setContentType(response.contentType)
          writeHeaders(request, response, servletResponse)

          servletResponse.setContentLength(body.length)

          val out = servletResponse.getOutputStream
          if (body.length > 0)
            out.write(body.toArray)
          out.close()

        case StreamResponse(status, headers, body) =>
          servletResponse.setStatus(status)
          servletResponse.setContentType(response.contentType)
          writeHeaders(request, response, servletResponse)

          // TODO: serve chunked response

          val outBuffer = ArrayBuffer.empty[Byte]

          var bytesCount = 0
          var totalBytes = 0
          val buffer = new Array[Byte](4 * 1024)

          do {
            bytesCount = body.read(buffer)
            if (bytesCount > 0) {
              totalBytes += bytesCount
              outBuffer.append(buffer.take(bytesCount) : _*)
            }
          } while(bytesCount > -1)

          servletResponse.setContentLength(totalBytes)

          val out = servletResponse.getOutputStream
          out.write(outBuffer.toArray)
          out.close()
      }
  }

  def init(filterConfig: FilterConfig) {}

  def destroy() {}
}

object ShifterFilter {
  final class ConcreteFilter private[api] (val routes: UrlRoutes, val ec: ExecutionContext) extends ShifterFilter

  def apply(routes: UrlRoutes)(implicit ec: ExecutionContext): ConcreteFilter =
    new ConcreteFilter(routes, ec)

  def apply(router: UrlRouter)(implicit ec: ExecutionContext): ConcreteFilter =
    new ConcreteFilter(router.route, ec)
}
