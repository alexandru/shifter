package shifter.http.client

import java.io.{InputStream, ByteArrayInputStream, UnsupportedEncodingException}
import collection.mutable.ArrayBuffer
import java.util
import util.concurrent.atomic.AtomicReference

class HttpClientResponse(val status: Int, val headers: Map[String, String], inputStream: InputStream) {
  private[this] val consumed = new AtomicReference(false)

  private[this] lazy val bytes =
    consumed.synchronized {
      if (consumed.get())
        throw new HttpClientException("Response was already consumed")

      consumed.set(true)

      val chunkBuffer = Array.fill(1024 * 4)(0.toByte)
      val buffer = ArrayBuffer.empty[Byte]
      var bytesRead = -1

      do {
        bytesRead = inputStream.read(chunkBuffer)
        if (bytesRead > 0)
          buffer.append(chunkBuffer : _*)
      } while(bytesRead > -1)

      buffer.toArray
    }

  def bodyAsArray =
    util.Arrays.copyOf(bytes, bytes.length)

  lazy val bodyAsStream =
    consumed.synchronized {
      if (!consumed.get()) {
        consumed.set(true)
        inputStream
      }
      else
        new ByteArrayInputStream(bytes)
    }

  def bodyAsString(encoding: String) =
    new String(bytes, encoding)

  lazy val bodyAsString =
    try {
      new String(bytes, contentEncoding)
    }
    catch {
      case ex: UnsupportedEncodingException =>
        try {
          new String(bytes, "UTF-8")
        }
        catch {
          case _: Exception => throw ex
        }
    }

  lazy val contentEncoding: String = {
    getHeader("content-type") match {
      case Some(EncodingFormat(charset)) => charset.toUpperCase
      case _ => "UTF-8"
    }
  }

  private[this] def getHeader(key: String) =
    headers.find(_._1.toLowerCase == key.toLowerCase)

  private[this] val EncodingFormat = """^.*[;]\s*charset[=]([a-zA-Z0-9-]+)$""".r
}
