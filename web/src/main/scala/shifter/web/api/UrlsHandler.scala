package shifter.web.api

import javax.servlet.{FilterConfig, FilterChain, ServletRequest, Filter}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import collection.mutable.ArrayBuffer
import org.apache.commons.codec.binary.Base64
import com.typesafe.scalalogging.slf4j.Logging
import com.yammer.metrics.Metrics
import java.util.concurrent.TimeUnit
import shifter.web.server.Configuration

trait UrlsHandler extends Filter with Logging {
  def applicableFor(request: Request): Boolean = true

  protected def onRequestEvent(request: Request) {
    activeRequests.map(_.inc())
    requests.map(_.mark())
  }

  protected def onResponseEvent(request: Request, response: Response) {
    activeRequests.map(_.dec())
  }

  protected[api] def writeHeaders(req: ServletRequest, resp: HttpServletResponse, ourHeaders: Map[String, String]) {
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

  protected[api] final def writeResponse(req: ServletRequest, resp: HttpServletResponse, chain: FilterChain, request: Request, result: Response) {
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

  // METRICS

  protected[api] lazy val httpConfig = Configuration.load()

  private[this] lazy val activeRequests =
    if (httpConfig.isInstrumented)
      Some(Metrics.newCounter(this.getClass, "active-requests"))
    else
      None

  private[this] val requests =
    if (httpConfig.isInstrumented)
      Some(Metrics.newMeter(this.getClass, "requests", "requests", TimeUnit.SECONDS))
    else
      None
}
