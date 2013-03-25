package shifter.http.client

import concurrent.{Promise, Future, ExecutionContext}
import com.ning.http.client._
import extra.ThrottleRequestFilter
import util._
import collection.JavaConverters._
import util.Success


class NingHttpClient private[client] (config: AsyncHttpClientConfig) extends HttpClient {

  def request(method: String, url: String, data: Map[String, String], headers: Map[String, String])(implicit ec: ExecutionContext): Future[HttpClientResponse] = {
    val request = prepareRequest(method, url, data)
    val futureResponse = makeRequest(url, request, headers)

    futureResponse.map { response =>
      val headersJavaMap = response.getHeaders

      var headers = Map.empty[String, String]
      for (header <- headersJavaMap.keySet.asScala) {
        // sometimes getJoinedValue() would be more correct.
        headers += (header -> headersJavaMap.getFirstValue(header))
      }

      new HttpClientResponse(response.getStatusCode, headers, response.getResponseBodyAsStream)
    }
  }


  // To Implement
  def request(method: String, url: String, body: Array[Byte], headers: Map[String, String])(implicit ec: ExecutionContext): Future[HttpClientResponse] = {
    val request = method match {
      case "GET" =>
        val getRequest = client.prepareGet(url)
        headers.foldLeft(getRequest) { case (acc, (k,v)) =>
          acc.addHeader(k,v)
        }

      case "POST" =>
        val postRequest = headers.foldLeft(client.preparePost(url)) {
          case (acc, (k,v)) =>
            acc.addHeader(k,v)
        }
        postRequest.setBody(body)
      case _ =>
        throw new Exception("Method not supported by HTTP Client: " + method)
    }

    val futureResponse = makeRequest(url, request, headers)

    futureResponse.map { response =>
      val headersJavaMap = response.getHeaders

      var headers = Map.empty[String, String]
      for (header <- headersJavaMap.keySet.asScala) {
        // sometimes getJoinedValue() would be more correct.
        headers += (header -> headersJavaMap.getFirstValue(header))
      }

      new HttpClientResponse(response.getStatusCode, headers, response.getResponseBodyAsStream)
    }
  }

  def close() {
    client.close()
  }

  private[this] def prepareRequest(method: String, url: String, data: Map[String, String]) =
    method match {
      case "GET" =>
        var request = client.prepareGet(url)
        data.foreach {
          case (k,v) =>
            request = request.addQueryParameter(k, v)
        }
        request

      case "POST" =>
        val request = client.preparePost(url)
          .addHeader("Content-Type", "application/x-www-form-urlencoded")
        val params = data.foldLeft(new FluentStringsMap) {
          (acc, e) => acc.add(e._1, e._2)
        }
        request.setParameters(params)

      case _ =>
        throw new Exception("Method not supported by HTTP Client: " + method)
    }

  private[this] def makeRequest(url: String, request: AsyncHttpClient#BoundRequestBuilder,
                                headers: Map[String, String]): Future[Response] = {
    val promise = Promise[Response]()
    val builder = new Response.ResponseBuilder()

    val httpHandler = new AsyncHandler[Unit] {
      @volatile
      var finished = false

      private def finish(body: => Unit) {
        if (!finished)
          try {
            body
          }
          catch {
            case t: Throwable =>
              promise.complete(Failure(t))
          }
          finally {
            finished = true
            assert(promise.isCompleted)
          }
      }

      def onThrowable(t: Throwable) {
        finish {
          throw t
        }
      }

      def onBodyPartReceived(bodyPart: HttpResponseBodyPart) = {
        builder.accumulate(bodyPart)
        AsyncHandler.STATE.CONTINUE
      }

      def onStatusReceived(responseStatus: HttpResponseStatus) = {
        builder.accumulate(responseStatus)
        AsyncHandler.STATE.CONTINUE
      }

      def onHeadersReceived(headers: HttpResponseHeaders) = {
        builder.accumulate(headers)
        AsyncHandler.STATE.CONTINUE
      }

      def onCompleted() {
        finish {
          val response = builder.build()
          promise.complete(Success(response))
        }
      }
    }

    // executing request, with auth headers if given
    val withHeaders = headers.foldLeft(request) { (acc, elem) =>
      acc.addHeader(elem._1, elem._2)
    }

    withHeaders.execute(httpHandler)
    promise.future
  }

  private[this] val client = new AsyncHttpClient(config)
}

object NingHttpClient {
  def apply(): NingHttpClient =
    apply(HttpClientConfig())

  def apply(config: HttpClientConfig): NingHttpClient = {
    val builder = new AsyncHttpClientConfig.Builder()

    val ningConfig = builder
      .setMaximumConnectionsTotal(config.maxTotalConnections)
      .setMaximumConnectionsPerHost(config.maxConnectionsPerHost)
      .addRequestFilter(new ThrottleRequestFilter(config.maxTotalConnections))
      .setRequestTimeoutInMs(config.requestTimeoutMs)
      .setConnectionTimeoutInMs(config.connectionTimeoutMs)
      .setAllowPoolingConnection(true)
      .setAllowSslConnectionPool(true)
      .setFollowRedirects(config.followRedirects)
      .build

    new NingHttpClient(ningConfig)
  }
}

