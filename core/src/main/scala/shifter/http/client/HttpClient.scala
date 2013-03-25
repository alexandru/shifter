package shifter.http.client

import concurrent._

trait HttpClient {
  // To Implement
  def request(
    method: String,
    url: String,
    data: Map[String, String],
    headers: Map[String, String]
  )(implicit ec: ExecutionContext): Future[HttpClientResponse]

  // To Implement
  def request(
    method: String,
    url: String,
    body: Array[Byte],
    headers: Map[String, String]
  )(implicit ec: ExecutionContext): Future[HttpClientResponse]

  // To Implement
  def close()

  def request(method: String, url: String)(implicit ec: ExecutionContext): Future[HttpClientResponse] =
    request(method, url, Map.empty[String, String])

  def request(method: String, url: String, data: Map[String, String])(implicit ec: ExecutionContext): Future[HttpClientResponse] =
    request(method, url, data, Map.empty[String, String])

  def requestWithAuth(method: String, url: String, data: Map[String, String], user: Option[String], password: Option[String])(implicit ec: ExecutionContext): Future[HttpClientResponse] = {
    val headers =
      for (u <- user; p <- password) yield {
        val auth = "Basic " + Base64.encode((u + ":" + p).getBytes("UTF-8"))
        Map("Authorization" -> auth)
      }

    request(method, url, data, headers.getOrElse(Map.empty))
  }
}

