package shifter.http.client

import concurrent._
import java.io.InputStream

trait HttpClient {
  def get(
    method: String,
    url: String,
    data: Map[String, String] = Map.empty,
    user: Option[String] = None,
    password: Option[String] = None,
    forceEncoding: Option[String] = None
  ): Future[HttpResult]

  def getStream(
    method: String,
    url: String,
    data: Map[String, String] = Map.empty,
    user: Option[String] = None,
    password: Option[String] = None
  ): Future[HttpStreamResult]

  def close()
}

case class HttpResult(status: Int, headers: Map[String, String], body: String)
case class HttpStreamResult(status: Int, headers: Map[String, String], body: InputStream)
