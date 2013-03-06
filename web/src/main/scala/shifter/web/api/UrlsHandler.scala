package shifter.web.api

import javax.servlet.{FilterConfig, FilterChain, ServletRequest, Filter}
import javax.servlet.http.HttpServletResponse
import collection.mutable.ArrayBuffer
import com.typesafe.scalalogging.slf4j.Logging
import com.yammer.metrics.Metrics
import java.util.concurrent.TimeUnit
import shifter.web.server.Configuration


trait UrlsHandler extends Filter with Logging {
  def applicableFor(request: HttpRequest[_]): Boolean = true

  protected def onRequestEvent(request: HttpRequest[_]) {
    activeRequests.map(_.inc())
    requests.map(_.mark())
  }

  protected def onResponseEvent(request: HttpRequest[_], response: HttpResponse[_]) {
    activeRequests.map(_.dec())
  }

  protected[api] def writeHeaders(request: HttpRequest[_], response: HttpResponse[_], servletResponse: HttpServletResponse) {
    for ((key, list) <- response.headers; value <- list)
      servletResponse.setHeader(key, value)

    if (!response.hasHeader("Cache-Control"))
      servletResponse.setHeader("Cache-Control", "must-revalidate,no-cache,no-store")

    if (!response.hasHeader("Connection"))
      if (request.protocol == "HTTP/1.1")
        servletResponse.setHeader("Connection", request.header("Connection").getOrElse("keep-alive"))
      else
        servletResponse.setHeader("Connection", request.header("Connection").getOrElse("close"))
  }

  protected[api] final def writeResponse(
      req: ServletRequest, resp: HttpServletResponse, chain: FilterChain,
      request: HttpRequest[_], response: HttpResponse[_]) {

    if (!resp.isCommitted) {
      onResponseEvent(request, response)

      response match {
        case HttpSimpleResponse(status, headers, body) =>
          resp.setStatus(status)
          resp.setContentType(response.contentType)
          resp.setCharacterEncoding("UTF-8")

          writeHeaders(request, response, resp)

          val bytes = body.getBytes("UTF-8")
          resp.setContentLength(bytes.length)

          val out = resp.getOutputStream
          if (bytes.length > 0)
            out.write(bytes)
          out.close()

        case HttpBytesResponse(status, headers, body) =>
          resp.setStatus(status)
          resp.setContentType(response.contentType)
          writeHeaders(request, response, resp)

          resp.setContentLength(body.length)

          val out = resp.getOutputStream
          if (body.length > 0)
            out.write(body.toArray)
          out.close()

        case HttpStreamedResponse(status, headers, body) =>
          resp.setStatus(status)
          resp.setContentType(response.contentType)
          writeHeaders(request, response, resp)

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
