package shifter.web.sample.controllers

import shifter.web.api2.mvc._

object Urls extends UrlRouter {
  def route = {
    case GET("/") =>
      Application.homepage

    case GET(p"/hello/") =>
      Application.hello

    case GET(p"/hello/$name/") =>
      Application.helloName(name)

    case GET(p"/async/hello/") =>
      Application.asyncHello
  }
}
