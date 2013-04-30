package shifter.http.client

case class HttpClientConfig(
  maxTotalConnections: Int = 2000,
  maxConnectionsPerHost: Int = 200,
  requestTimeoutMs: Int = 15000,
  connectionTimeoutMs: Int = 4000,
  followRedirects: Boolean = true
)