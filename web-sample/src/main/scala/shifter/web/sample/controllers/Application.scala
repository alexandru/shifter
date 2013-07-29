package shifter.web.sample.controllers

import shifter.web.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Future

object Application extends Controller {
  val homepage = Action {
    Ok("Welcome to your new app!")
  }

  val hello = Action { implicit req =>
    val name = req.queryParam("name").getOrElse("Anonymous")
    helloName(name)(req)
  }

  def helloName(name: String) = Action {
    Ok("Hello, %s!".format(name))
  }

  val asyncHello = Action { req =>
    val response = Future {
      Thread.sleep(1000)
      val name = req.queryParam("name").getOrElse("Anonymous")
      Ok("Hello, %s!".format(name))
    }

    Async(response, timeout = 1.second)
  }
}
