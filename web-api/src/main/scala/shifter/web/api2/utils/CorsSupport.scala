package shifter.web.api2.utils

import shifter.web.api2.mvc._
import shifter.web.api2.responses._
import shifter.web.api2.requests.{BodyParser, Request}

trait CorsSupport extends ResultBuilders {
  def CorsAction(cb: => Result): Action[String] =
    Action(cb.withHeaders(actionCorsHeaders: _*))

  def CorsAction(cb: Request[String] => Result): Action[String] =
    Action { request =>
      cb(request).withHeaders(actionCorsHeaders: _*)
    }

  def CorsAction[A](bp: BodyParser[A])(f: Request[A] => Result): Action[A] =
    Action(bp) { request =>
      f(request).withHeaders(actionCorsHeaders: _*)
    }

  private[this] val actionCorsHeaders = Seq(
    "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS, HEAD, PUT, DELETE",
    "Access-Control-Allow-Credentials" -> "true",
    "Access-Control-Allow-Headers" -> "ORIGIN, CONTENT-TYPE, X-REQUESTED-WITH, *",
    "Access-Control-Allow-Origin" -> "*",
    "Access-Control-Max-Age" -> "30000"
  )

  private[this] val optionsCorsHeaders = actionCorsHeaders ++ Seq(
    "Content-Type" -> "text/plain",
    "Allow" -> "GET, POST, OPTIONS, HEAD, PUT, DELETE"
  )

  val optionsResponse = Ok.withHeaders(optionsCorsHeaders : _*)
}
