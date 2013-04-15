package shifter.web.sample.controllers

import shifter.web.api.mvc._
import scala.concurrent.{Promise, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import shifter.concurrency.extensions._
import shifter.web.api.responses.SimpleResponse
import scala.util.Success

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
