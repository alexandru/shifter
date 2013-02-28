package shifter.http.client

case class HttpClientConfig(
  maxTotalConnections: Int = 2000,
  maxConnectionsPerHost: Int = 200,
  requestTimeoutMs: Int = 15000,
  connectionTimeoutMs: Int = 1000,
  followRedirects: Boolean = true
)