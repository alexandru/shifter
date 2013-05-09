package shifter.web.sample

import shifter.web.jetty9.{FilterConfig, Context, LifeCycle}
import shifter.web.api.ShifterFilter
import shifter.web.sample.controllers.Urls
import com.typesafe.scalalogging.slf4j.Logging

class ServerLifeCycle extends LifeCycle with Logging {
  def createContext: Context =
    Context(filters = List(
      FilterConfig("sample", ShifterFilter(Urls), "/*")
    ))

  def destroyContext(ctx: Context) {
    logger.info("Server is shutting down!")
  }
}
