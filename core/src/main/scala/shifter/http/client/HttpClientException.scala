package shifter.http.client


class HttpClientException(msg: String, cause: Throwable)
    extends RuntimeException(msg, cause) {

  def this(msg: String) = this(msg, null)
}