package shifter.web.api.requests.parsers

import shifter.web.api.requests._
import shifter.web.api.http._
import shifter.web.api.responses.{ResultBuilders, Result}
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.apache.commons.fileupload.FileItem
import collection.mutable
import collection.JavaConverters._


object MultiPartParser extends BodyParser[MultiPartBody] with ResultBuilders {
  def canBeParsed(raw: RawServletRequest): Boolean =
    validMethods(raw.method) && validContentTypes(raw.contentType)

  def apply(r: RequestHeader): Either[Result, MultiPartBody] =
    r match {
      case raw: RawServletRequest =>
        if (canBeParsed(raw)) {
          val items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(raw.underlying)
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

          Right(MultiPartBody(
            params = params,
            files = fileItems
          ))
        }
        else
          Left(BadRequest)

      case _ =>
        Left(BadRequest)
    }

  val validMethods = Set(
    HttpMethod.POST,
    HttpMethod.PUT
  )

  val validContentTypes = Set(
    "multipart/form-data"
  )
}
