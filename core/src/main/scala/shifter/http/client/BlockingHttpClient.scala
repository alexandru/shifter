package shifter.http.client

import concurrent._
import java.net.{HttpURLConnection, URL}
import java.net.URLEncoder.encode
import collection.JavaConverters._


class BlockingHttpClient extends HttpClient {

  // To Implement
  def request(method: String, url: String, data: Map[String, String], headers: Map[String, String]): Future[HttpClientResponse] =
    Future {
      val connection: HttpURLConnection =
        method match {
          case "POST" =>
            val connection = new URL(url).openConnection()
              .asInstanceOf[HttpURLConnection]
            connection.setDoOutput(true) // triggers post
            connection.setRequestProperty("Accept-Charset", "utf-8")
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
            // writing body
            val out = connection.getOutputStream
            out.write(prepareQuery(data).getBytes("utf-8"))
            out.close()
            connection

          case "GET" =>
            val connection = new URL(url + "?" + prepareQuery(data)).openConnection()
              .asInstanceOf[HttpURLConnection]
            connection.setRequestProperty("Accept-Charset", "utf-8")
            connection
        }

      for ((k,v) <- headers)
        connection.setRequestProperty(k,v)

      val stream = connection.getInputStream
      val status = connection.getResponseCode
      val responseHeaders =
        connection.getHeaderFields.asScala.foldLeft(Map.empty[String, String]) { (acc, elem) =>
          if (elem._1 != null && elem._2 != null) {
            val (key, value) = (elem._1, elem._2.asScala.find(v => v != null && !v.isEmpty).headOption)

            if (value.isDefined)
              acc.updated(key, value.get)
            else
              acc
          }
          else
            acc
        }

      new HttpClientResponse(status, responseHeaders, stream)
    }


  // To Implement
  def request(method: String, url: String, body: Array[Byte], headers: Map[String, String]): Future[HttpClientResponse] =
    Future {
      val connection: HttpURLConnection =
        method match {
          case "POST" =>
            val connection = new URL(url).openConnection()
              .asInstanceOf[HttpURLConnection]
            connection.setDoOutput(true) // triggers post

            for ((k,v) <- headers)
              connection.setRequestProperty(k,v)

            // writing body
            val out = connection.getOutputStream
            out.write(body)
            out.close()
            connection

          case "GET" =>
            val connection = new URL(url).openConnection()
              .asInstanceOf[HttpURLConnection]
            for ((k,v) <- headers)
              connection.setRequestProperty(k,v)
            connection
        }

      val stream = connection.getInputStream
      val status = connection.getResponseCode
      val responseHeaders =
        connection.getHeaderFields.asScala.foldLeft(Map.empty[String, String]) { (acc, elem) =>
          val (key, value) = (elem._1, elem._2.asScala.find(v => v != null && !v.isEmpty).headOption)

          if (value.isDefined)
            acc.updated(key, value.get)
          else
            acc
        }

      new HttpClientResponse(status, responseHeaders, stream)
    }


  private[this] def prepareQuery(data: Map[String, String]): String =
    data.map { case (k,v) => encode(k, "UTF-8") + "=" + encode(v, "UTF-8") }
      .mkString("&")

  def close() {}
}
