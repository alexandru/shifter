package shifter.web.api.responses

import language.existentials
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import java.io.InputStream
import shifter.web.api.http._

sealed trait Result {
  def replacedHeaders(headers: Map[String, Seq[String]]): Result

  def withHeaders(list: (String, String)*): Result
  def withHeader(h: (String, String)): Result
  def addHeader(h: (String, String)): Result

  def addCookie(name: String, value: String): Result =
    addHeader(Cookie(name, value).toHeader)

  def addCookie(name: String, value: String, expiresSecs: Int): Result =
    addHeader(Cookie(name, value, expiresSecs = Some(expiresSecs)).toHeader)

  def addCookie(cookie: Cookie): Result =
    addHeader(cookie.toHeader)

  def deleteHeader(key: String): Result
}

case class AsyncResult private[responses] (
  result: Future[CompleteResult],
  ec: ExecutionContext,
  timeout: Duration = 10.seconds,
  timeoutResponse: () => CompleteResult
)
extends Result {
  def replacedHeaders(headers: Map[String, Seq[String]]): AsyncResult = {
    val newResponse = result.map(_.replacedHeaders(headers))(ec)
    AsyncResult(newResponse, ec, timeout, timeoutResponse)
  }

  def withHeaders(list: (String, String)*): AsyncResult = {
    val newResponse = result.map(_.withHeaders(list :_*))(ec)
    AsyncResult(newResponse, ec, timeout, timeoutResponse)
  }

  def withHeader(h: (String, String)): AsyncResult = {
    val newResponse = result.map(_.withHeader(h))(ec)
    AsyncResult(newResponse, ec, timeout, timeoutResponse)
  }

  def addHeader(h: (String, String)): AsyncResult = {
    val newResponse = result.map(_.addHeader(h))(ec)
    AsyncResult(newResponse, ec, timeout, timeoutResponse)
  }

  def deleteHeader(key: String): AsyncResult = {
    val newResponse = result.map(_.deleteHeader(key))(ec)
    AsyncResult(newResponse, ec, timeout, timeoutResponse)
  }

  def withTimeoutResponse(cb: => CompleteResult) =
    AsyncResult(result, ec, timeout, () => cb)
}

sealed trait CompleteResult extends Result {
  def status: Int
  def headers: Map[String, Seq[String]]

  lazy val contentType =
    headers.find(_._1.toUpperCase == "CONTENT-TYPE")
      .flatMap(_._2.headOption).getOrElse("text/html")

  def hasHeader(key: String) = {
    val upper = key.toUpperCase
    headers.keySet.exists(_.toUpperCase == upper)
  }

  def replacedHeaders(headers: Map[String, Seq[String]]): CompleteResult

  def withHeaders(list: (String, String)*): CompleteResult =
    replacedHeaders(list.foldLeft(headers) { case (acc, (key, value)) =>
      if (key != null && value != null) {
        val oldList = acc.get(key).getOrElse(Vector.empty)
        acc.updated(key, oldList :+ value)
      }
      else
        acc
    })

  def withHeader(h: (String, String)): CompleteResult = {
    val newHeaders = headers.updated(h._1, Seq(h._2))
    replacedHeaders(newHeaders)
  }

  def addHeader(h: (String, String)): CompleteResult = {
    val oldList = headers.get(h._1).getOrElse(Vector.empty)
    val newHeaders = headers.updated(h._1, oldList :+ h._2)
    replacedHeaders(newHeaders)
  }

  def deleteHeader(key: String): CompleteResult = {
    val newHeaders = headers - key
    replacedHeaders(newHeaders)
  }
}

case class BytesResult(status: Int, headers: Map[String, Seq[String]] = Map.empty, body: Seq[Byte])
  extends CompleteResult {

  def replacedHeaders(headers: Map[String, Seq[String]]): CompleteResult =
    copy(headers = headers)
}

case class SimpleResult(status: Int, headers: Map[String, Seq[String]] = Map.empty, body: String = "")
  extends CompleteResult {

  def replacedHeaders(headers: Map[String, Seq[String]]): CompleteResult =
    copy(headers = headers)
}

case class StreamedResult(status: Int, headers: Map[String, Seq[String]] = Map.empty, body: InputStream)
  extends CompleteResult {

  def replacedHeaders(headers: Map[String, Seq[String]]): CompleteResult =
    copy(headers = headers)
}

