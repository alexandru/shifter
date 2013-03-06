package shifter.web.api

import java.io.InputStream
import concurrent.Future


sealed trait HttpResponse[T] {
  def status: Int
  def headers: Map[String, Seq[String]]
  def body: T

  lazy val contentType =
    headers.find(_._1.toUpperCase == "CONTENT-TYPE")
      .flatMap(_._2.headOption).getOrElse("text/html")

  def replacedHeaders(headers: Map[String, Seq[String]]): HttpResponse[T]

  def hasHeader(key: String) = {
    val upper = key.toUpperCase
    headers.keySet.find(_.toUpperCase == upper).isDefined
  }

  def addHeader(h: (String, String)): HttpResponse[T] = {
    val lst = headers.get(h._1).getOrElse(Seq.empty)
    val newHeaders = headers.updated(h._1, lst :+ h._2)
    replacedHeaders(newHeaders)
  }

  def withHeader(h: (String, String)): HttpResponse[T] =
    replacedHeaders(headers.updated(h._1, Seq(h._2)))

  def withHeaders(headers: Map[String, String]): HttpResponse[T] =
    replacedHeaders(headers.foldLeft(this.headers) { (acc, elem) =>
      acc.updated(elem._1, Seq(elem._2))
    })

  def addCookie(name: String, value: String): HttpResponse[T] =
    addHeader(Cookie(name, value).toHeader)

  def addCookie(name: String, value: String, expiresSecs: Int): HttpResponse[T] =
    addHeader(Cookie(name, value, expiresSecs = Some(expiresSecs)).toHeader)

  def addCookie(cookie: Cookie): HttpResponse[T] =
    addHeader(cookie.toHeader)

  def asFuture: Future[HttpResponse[T]] =
    Future.successful(this)
}

case class HttpSimpleResponse(status: Int, headers: Map[String, Seq[String]] = Map.empty, body: String = "")
    extends HttpResponse[String] {
  def replacedHeaders(headers: Map[String, Seq[String]]): HttpResponse[String] =
    copy(headers = headers)
}

case class HttpStreamedResponse(status: Int, headers: Map[String, Seq[String]] = Map.empty, body: InputStream)
    extends HttpResponse[InputStream] {

  def replacedHeaders(headers: Map[String, Seq[String]]): HttpResponse[InputStream] =
    copy(headers = headers)
}

case class HttpBytesResponse(status: Int, headers: Map[String, Seq[String]] = Map.empty, body: Seq[Byte])
  extends HttpResponse[Seq[Byte]] {

  def replacedHeaders(headers: Map[String, Seq[String]]): HttpResponse[Seq[Byte]] =
    copy(headers = headers)
}



