package shifter.web.api.responses

import language.existentials
import scala.concurrent.Future
import scala.concurrent.duration._
import shifter.web.api.base.Cookie
import java.io.InputStream
import shifter.web.api.mvc._

sealed trait HttpResponse[T]

case class AsyncResponse[T] private[responses] (
  response: Future[CompleteResponse[T]],
  timeout: Duration = 1.second,
  timeoutResponse: CompleteResponse[_] =
    ResponseBuilders.RequestTimeout("408 Timeout")
      .withHeader("Content-Type" -> "text/plain")
) extends HttpResponse[T]

case class ForwardResponse private[responses] (action: Action)
  extends HttpResponse[Action]

case object ChainResponse extends HttpResponse[Nothing]

sealed trait CompleteResponse[T] extends HttpResponse[T] {
  def status: Int
  def headers: Map[String, Seq[String]]
  def body: T

  lazy val contentType =
    headers.find(_._1.toUpperCase == "CONTENT-TYPE")
      .flatMap(_._2.headOption).getOrElse("text/html")

  def replacedHeaders(headers: Map[String, Seq[String]]): CompleteResponse[T]

  def hasHeader(key: String) = {
    val upper = key.toUpperCase
    headers.keySet.find(_.toUpperCase == upper).isDefined
  }

  def addHeader(h: (String, String)): CompleteResponse[T] = {
    val lst = headers.get(h._1).getOrElse(Seq.empty)
    val newHeaders = headers.updated(h._1, lst :+ h._2)
    replacedHeaders(newHeaders)
  }

  def withHeader(h: (String, String)): CompleteResponse[T] =
    replacedHeaders(headers.updated(h._1, Seq(h._2)))

  def withHeaders(headers: Map[String, String]): CompleteResponse[T] =
    replacedHeaders(headers.foldLeft(this.headers) { (acc, elem) =>
      acc.updated(elem._1, Seq(elem._2))
    })

  def addCookie(name: String, value: String): CompleteResponse[T] =
    addHeader(Cookie(name, value).toHeader)

  def addCookie(name: String, value: String, expiresSecs: Int): CompleteResponse[T] =
    addHeader(Cookie(name, value, expiresSecs = Some(expiresSecs)).toHeader)

  def addCookie(cookie: Cookie): CompleteResponse[T] =
    addHeader(cookie.toHeader)

  def asFuture: Future[CompleteResponse[T]] =
    Future.successful(this)
}

case class BytesResponse(status: Int, headers: Map[String, Seq[String]] = Map.empty, body: Seq[Byte])
  extends CompleteResponse[Seq[Byte]] {

  def replacedHeaders(headers: Map[String, Seq[String]]): CompleteResponse[Seq[Byte]] =
    copy(headers = headers)
}

case class SimpleResponse(status: Int, headers: Map[String, Seq[String]] = Map.empty, body: String = "")
  extends CompleteResponse[String] {

  def replacedHeaders(headers: Map[String, Seq[String]]): CompleteResponse[String] =
    copy(headers = headers)
}

case class StreamResponse(status: Int, headers: Map[String, Seq[String]] = Map.empty, body: InputStream)
  extends CompleteResponse[InputStream] {

  def replacedHeaders(headers: Map[String, Seq[String]]): CompleteResponse[InputStream] =
    copy(headers = headers)
}

