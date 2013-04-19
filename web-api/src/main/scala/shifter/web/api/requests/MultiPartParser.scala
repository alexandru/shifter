package shifter.web.api.requests

import shifter.web.api.http.{FileInfo, HttpMethod, MultiPartBody}
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import scala.collection.mutable
import org.apache.commons.fileupload.FileItem
import collection.JavaConverters._

object MultiPartParser extends RequestParser[MultiPartBody, MultiPartRequest] {
  val validMethods = Set(
    HttpMethod.POST,
    HttpMethod.PUT
  )

  val validContentTypes = Set(
    "multipart/form-data"
  )

  def canBeParsed(raw: RawRequest): Boolean =
    validMethods(raw.method) && validContentTypes(raw.contentType)


  def parse(raw: RawRequest): Option[MultiPartRequest] =
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

      Some(MultiPartRequest(
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