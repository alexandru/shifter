package shifter.http.client

import java.io.{ByteArrayInputStream, UnsupportedEncodingException}

class HttpClientResponse(val status: Int, val headers: Map[String, String], bytesArray: Array[Byte]) {
  def bodyAsArray =
    bytesArray.clone()

  lazy val bodyAsStream =
    new ByteArrayInputStream(bytesArray)

  def bodyAsString(encoding: String) =
    new String(bytesArray, encoding)

  lazy val bodyAsString =
    try {
      new String(bytesArray, contentEncoding)
    }
    catch {
      case ex: UnsupportedEncodingException =>
        try {
          new String(bytesArray, "UTF-8")
        }
        catch {
          case _: Exception => throw ex
        }
    }

  lazy val contentEncoding: String = {
    getHeader("content-type") match {
      case Some(EncodingFormat(charset)) => charset.toUpperCase
      case None => "UTF-8"
    }
  }

  private[this] def getHeader(key: String) =
    headers.find(_._1.toLowerCase == key.toLowerCase)

  private[this] val EncodingFormat = """^.*[;]\s*charset[=]([a-zA-Z0-9-]+)$""".r
}

object HttpClientResult {
  def unapply(obj: HttpClientResponse): Option[(Int, Map[String, String], String)] =
    Some((obj.status, obj.headers, obj.bodyAsString))
}