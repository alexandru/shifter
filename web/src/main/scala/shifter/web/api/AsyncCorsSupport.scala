package shifter.web.api

import concurrent.{ExecutionContext, Future}

trait AsyncCorsSupport {
  val corsHeaders = Map(
    "Content-Type" -> "text/plain",
    "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS, HEAD, PUT, DELETE",
    "Access-Control-Allow-Credentials" -> "true",
    "Access-Control-Allow-Headers" -> "ORIGIN, CONTENT-TYPE, X-REQUESTED-WITH, *",
    "Access-Control-Allow-Origin" -> "*",
    "Access-Control-Max-Age" -> "30000",
    "Allow" -> "GET, POST, OPTIONS"
  )

  def withCorsHeaders(request: HttpRequest[_], allowedMethods: HttpMethod.Value*)(cb: => Future[HttpResponse[_]])(implicit ec: ExecutionContext): Future[HttpResponse[_]] = {
    if (request.method == HttpMethod.OPTIONS)
      Ok("").withHeaders(corsHeaders).asFuture
    else
      cb.map(_.withHeader("Access-Control-Allow-Origin" -> "*"))
  }
}
