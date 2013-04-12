package shifter.web.sample.controllers

import shifter.web.api.mvc._

object Urls extends UrlRouter {

  def route = {
    case GET("/") =>
      Application.homepage

    case GET(Path("hello" :: Nil)) =>
      Application.hello

    case GET(Path("hello" :: name :: Nil)) =>
      Application.helloName(name)

    case GET(Path("async" :: "hello" :: Nil)) =>
      Application.asyncHello
  }
}
