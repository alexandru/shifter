package shifter.web

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.commons.codec.binary.Base64
import concurrent.Future
import org.slf4j.LoggerFactory
import collection.mutable.ArrayBuffer
import concurrent.ExecutionContext.Implicits.global


trait BaseUrlsRouter extends Filter {

  def applicableFor(request: Request): Boolean = true
  def routes: PartialFunction[Request, Future[Response]]

  protected def requestTimeout: Int = 10000

  protected def triggeredTimeoutResponse(request: Request): Response =
    HttpRequestTimeout("408 Timeout", "text/plain")

  protected def onRequestEvent(request: Request) {
    // does nothing by default
  }

  protected def onResponseEvent(request: Request, response: Response) {
    // does nothing by default
  }

  protected def onRequestTimeout(request: Request) {
    // does nothing by default
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

  private[this] def writeHeaders(req: ServletRequest, resp: HttpServletResponse, ourHeaders: Map[String, String]) {
    val request = req.asInstanceOf[HttpServletRequest]
    ourHeaders.foreach { case (key, value) => resp.setHeader(key, value) }

    if (!ourHeaders.contains("Cache-Control"))
      resp.setHeader("Cache-Control", "must-revalidate,no-cache,no-store")

    if (!ourHeaders.contains("Connection"))
      if (request.getProtocol == "HTTP/1.1")
        resp.setHeader("Connection", Option(request.getHeader("Connection")).getOrElse("keep-alive"))
      else
        resp.setHeader("Connection", Option(request.getHeader("Connection")).getOrElse("close"))
  }

  private[this] final def writeResponse(req: ServletRequest, resp: HttpServletResponse, chain: FilterChain, request: Request, result: Response) {
    if (!resp.isCommitted) {
      onResponseEvent(request, result)

      result match {
        case StreamResponse(status, headers, body) =>
          resp.setStatus(status)
          writeHeaders(req, resp, headers)

          val outBuffer = ArrayBuffer.empty[Byte]

          var bytesCount = 0
          var totalBytes = 0
          val buffer = Array.fill(4 * 1024){0.toByte}

          do {
            bytesCount = body.read(buffer)
            if (bytesCount > 0) {
              totalBytes += bytesCount
              outBuffer.append(buffer.take(bytesCount) : _*)
            }
          } while(bytesCount > -1)

          resp.setContentLength(totalBytes)

          val out = resp.getOutputStream
          out.write(outBuffer.toArray)
          out.close()

        case HttpError(status, body, contentType, headers) =>
          resp.setStatus(status)
          resp.setContentType(contentType)
          resp.setCharacterEncoding("UTF-8")

          writeHeaders(req, resp, headers)

          val bytes = body.getBytes("UTF-8")
          resp.setContentLength(bytes.length)

          val out = resp.getOutputStream
          if (bytes.length > 0)
            out.write(bytes)
          out.close()

        case HttpOk(body, contentType, headers) =>
          resp.setStatus(200)
          resp.setContentType(contentType)
          resp.setCharacterEncoding("UTF-8")

          writeHeaders(req, resp, headers)

          val bytes = body.getBytes("UTF-8")
          resp.setContentLength(bytes.length)

          val out = resp.getOutputStream
          if (bytes.length > 0)
            out.write(bytes)
          out.close()

        case Pixel =>
          resp.setStatus(200)
          resp.setContentType("image/gif")
          writeHeaders(req, resp, Map.empty)
          val pixel = "R0lGODlhAQABAPAAAAAAAAAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw"

          val bytes = Base64.decodeBase64(pixel)
          resp.setContentLength(bytes.length)

          val out = resp.getOutputStream
          out.write(bytes)
          out.close()

        case HttpRedirect(url, status) =>
          resp.setStatus(if (status == 301 || status == 302) status else 302)
          resp.setHeader("Location", url)
          resp.setContentLength(0)
          writeHeaders(req, resp, Map.empty)
      }
    }
  }

  def init(filterConfig: FilterConfig) {}
  def destroy() {}

  private[this] lazy val logger = LoggerFactory.getLogger(this.getClass)
}
