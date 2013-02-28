package shifter.http.client

import concurrent._
import java.io.InputStream
import com.ning.http.client.AsyncHttpClientConfig
import com.ning.http.client.extra.ThrottleRequestFilter

trait HttpClient {
  def request(
    method: String,
    url: String,
    data: Map[String, String] = Map.empty,
    user: Option[String] = None,
    password: Option[String] = None
  ): Future[HttpClientResponse]

  def close()
}

object HttpClient {
  def apply(): HttpClient =
    apply(HttpClientConfig())

  def apply(config: HttpClientConfig): HttpClient = {
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
