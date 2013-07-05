package shifter.http.client

import java.io.{InputStream, ByteArrayInputStream, UnsupportedEncodingException}
import collection.mutable.ArrayBuffer
import java.util
import scala.util.Try
import scala.concurrent.atomic.Atomic

class HttpClientResponse(val status: Int, val headers: Map[String, String], inputStream: InputStream) {
  private[this] val consumed = Atomic(initialValue = false)

  private[this] lazy val bytes =
    consumed.synchronized {
      if (consumed.get)
        throw new HttpClientException("Response was already consumed")

      consumed.set(true)

      val contentLength = headers.find(_._1.toUpperCase == "CONTENT-LENGTH")
        .flatMap(x => Try(x._2.toInt).toOption)

      contentLength match {
        case Some(length) =>
          val buffer = new Array[Byte](length)
          var remaining = length
          var offset = 0
          var bytesRead = -1

          do {
            assert(offset < length)

            bytesRead = inputStream.read(buffer, offset, remaining)
            if (bytesRead > 0) {
              offset += bytesRead
              remaining -= bytesRead
            }
          } while (bytesRead > -1 && remaining > 0)

          inputStream.close()
          buffer

        case _ =>
          // TODO: fix
          val chunkBuffer = new Array[Byte](1024 * 4)
          val buffer = ArrayBuffer.empty[Byte]
          var bytesRead = -1

          do {
            bytesRead = inputStream.read(chunkBuffer)
            if (bytesRead > 0)
              buffer.append(chunkBuffer.take(bytesRead) : _*)
          } while(bytesRead > -1)

          inputStream.close()
          buffer.toArray
      }
    }

  def bodyAsArray =
    util.Arrays.copyOf(bytes, bytes.length)

  lazy val bodyAsStream =
    consumed.synchronized {
      if (!consumed.get) {
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
