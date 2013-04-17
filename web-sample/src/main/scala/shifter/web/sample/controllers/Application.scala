package shifter.web.sample.controllers

import shifter.web.api.mvc._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Application extends Controller {

  val homepage = Action {
    Ok("Welcome to your new app!")
  }

  val hello = Action { implicit req =>
    val name = req.queryParam("name").getOrElse("Anonymous")
    Forward(helloName(name))
  }

  def helloName(name: String) = Action {
    Ok("Hello, %s!".format(name))
  }

  val asyncHello = Action { req =>
    val response = Future {
      val name = req.queryParam("name").getOrElse("Anonymous")
      Ok("Hello, %s!".format(name))
    }

    Async(response, timeout = 30.millis)
  }
}
