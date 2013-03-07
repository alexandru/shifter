package shifter.web.api

import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import collection.JavaConverters._
import collection.mutable
import org.apache.commons.fileupload.FileItem


case class HttpMultiPartFormRequest(
  method: HttpMethod.Value,
  path: String,
  domain: String,
  port: Int,
  protocol: String,
  url: String,
  query: Option[String],
  headers: Map[String, Seq[String]],
  remoteAddress: String,
  cookies: Map[String, Cookie],
  body: MultiPartBody
)
extends HttpRequest[MultiPartBody]


object HttpMultiPartFormRequest extends RequestParser[MultiPartBody, HttpMultiPartFormRequest] {
  val validMethods = Set(
    HttpMethod.POST,
    HttpMethod.PUT
  )

  val validContentTypes = Set(
    "multipart/form-data"
  )

  def canBeParsed(raw: HttpRawRequest): Boolean =
    validMethods(raw.method) && validContentTypes(raw.contentType)


  def parse(raw: HttpRawRequest): Option[HttpMultiPartFormRequest] =
    if (canBeParsed(raw)) {
      val items = new ServletFileUpload(new DiskFileItemFactory).parseRequest(raw.body)
        .asScala.asInstanceOf[mutable.Buffer[FileItem]]

      var params = Map.empty[String, Seq[String]]
      var fileItems = Map.empty[String, FileInfo]

      for (item <- items) {
        if (item.isFormField) {
          val key = item.getFieldName
          val value = item.getString("UTF-8")
          val oldList = params.get(key).getOrElse(Seq.empty)
          params = params.updated(key, oldList :+ value)
        }
        else {
          val fieldName = item.getFieldName
          val filename = item.getName
          val in = item.getInputStream
          val contentType = item.getContentType
          val size = item.getSize
          fileItems = fileItems.updated(fieldName, FileInfo(filename, in, contentType, size))
        }
      }

      Some(HttpMultiPartFormRequest(
        method = raw.method,
        path = raw.path,
        domain = raw.domain,
        port = raw.port,
        protocol = raw.protocol,
        url = raw.url,
        query = raw.query,
        headers = raw.headers,
        remoteAddress = raw.remoteAddress,
        cookies = raw.cookies,
        body = MultiPartBody(
          params = params,
          files = fileItems
        )
      ))
    }
    else
      None
}