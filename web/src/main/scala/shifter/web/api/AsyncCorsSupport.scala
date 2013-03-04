package shifter.web.api

import concurrent.{ExecutionContext, Future}
import concurrent.Future._

trait AsyncCorsSupport {
  def withCorsHeaders(request: Request, allowedMethods: String*)(cb: => Future[Response])(implicit ec: ExecutionContext): Future[Response] = {
    val allAllowedMethods = if (allowedMethods.exists(_ == "OPTIONS"))
      allowedMethods
    else
      allowedMethods :+ "OPTIONS"

    if (request.method == "OPTIONS")
      successful(HttpOk("", "text/plain", Map(
        "Access-Control-Allow-Methods" -> allAllowedMethods.mkString(", "),
        "Access-Control-Allow-Credentials" -> "true",
        "Access-Control-Allow-Headers" -> "ORIGIN, CONTENT-TYPE, X-REQUESTED-WITH, *",
        "Access-Control-Allow-Origin" -> "*",
        "Access-Control-Max-Age" -> "30000",
        "Allow" -> "GET, POST, OPTIONS"
      )))

    else if (!allAllowedMethods.exists(_ == request.method))
      successful(HttpMethodNotAllowed(allAllowedMethods.mkString(", ")))

    else
      cb.map {
        case resp : HttpOk =>
          resp.copy(headers=resp.headers ++ Map(
            "Access-Control-Allow-Origin" -> "*"
          ))
        case resp : HttpError =>
          resp.copy(headers=resp.headers ++ Map(
            "Access-Control-Allow-Origin" -> "*"
          ))
        case resp =>
          resp
      }
  }
}
