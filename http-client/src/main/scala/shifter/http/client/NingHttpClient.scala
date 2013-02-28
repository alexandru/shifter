package shifter.http.client

import concurrent.{Promise, Future, ExecutionContext}
import com.ning.http.client._
import extra.ThrottleRequestFilter
import com.ning.http.util.Base64
import util._
import collection.JavaConverters._
import util.Success
import ExecutionContext.Implicits.global


class NingHttpClient extends HttpClient {
  def get(method: String, url: String, data: Map[String, String], user: Option[String], password: Option[String], forceEncoding: Option[String]): Future[HttpResult] = {
    val request = prepareRequest(method, url, data)
    val futureResponse = makeRequest(url, request, user, password)

    futureResponse.map { response =>
      val headersJavaMap = response.getHeaders

      var headers = Map.empty[String, String]
      for (header <- headersJavaMap.keySet.asScala) {
        // sometimes getJoinedValue() would be more correct.
        headers += (header -> headersJavaMap.getFirstValue(header))
      }

      val body = forceEncoding match {
        case Some(enc) =>
          response.getResponseBody(enc)
        case None =>
          response.getResponseBody
      }

      HttpResult(response.getStatusCode, headers, body)
    }
  }

  def getStream(method: String, url: String, data: Map[String, String], user: Option[String], password: Option[String]): Future[HttpStreamResult] = {
    val request = prepareRequest(method, url, data)
    val futureResponse = makeRequest(url, request, user, password)

    futureResponse.map { response =>
      val headersJavaMap = response.getHeaders

      var headers = Map.empty[String, String]
      for (header <- headersJavaMap.keySet.asScala) {
        // sometimes getJoinedValue() would be more correct.
        headers += (header -> headersJavaMap.getFirstValue(header))
      }

      HttpStreamResult(response.getStatusCode, headers, response.getResponseBodyAsStream)
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
                                user: Option[String], password: Option[String]): Future[Response] = {
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
    for (u <- user; p <- password) {
      val authorization = "Basic " + Base64.encode((u + ":" + p).getBytes("UTF-8"))
      request.setHeader("Authorization", authorization)
    }

    request.execute(httpHandler)
    promise.future
  }

  private[this] val client = {
    val builder = new AsyncHttpClientConfig.Builder()

    val clientConfig = builder
      .setMaximumConnectionsTotal(1000)
      .setMaximumConnectionsPerHost(200)
      .addRequestFilter(new ThrottleRequestFilter(200))
      .setRequestTimeoutInMs(10000)
      .setAllowPoolingConnection(true)
      .setAllowSslConnectionPool(true)
      .setFollowRedirects(true)
      .build

    new AsyncHttpClient(clientConfig)
  }
}
