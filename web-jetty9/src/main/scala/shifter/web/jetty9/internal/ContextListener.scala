package shifter.web.jetty9.internal

import javax.servlet._
import com.typesafe.scalalogging.slf4j.Logging
import java.{util => jutil}
import shifter.web.jetty9.{Context, LifeCycle, Configuration}
import shifter.reflection.toInstance
import scala.util.{Failure, Success}

class ContextListener extends ServletContextListener with Logging {
  @volatile
  private[this] var context: Option[Context] = None

  def contextInitialized(sce: ServletContextEvent) {
    this.synchronized {
      context = Option(lifecycleInstance.createContext)
      val servletContext = sce.getServletContext

      for (router <- context.get.filters)
        addFilterMapping(router.name, router.instance, router.urlPattern, servletContext)

      for (router <- context.get.servlets)
        addServletMapping(router.name, router.instance, router.urlPattern, servletContext)

      logger.info("Servlet context was initialized")
    }
  }

  def contextDestroyed(sce: ServletContextEvent) {
    this.synchronized {
      if (context.isDefined) {
        logger.info("Servlet context is being destroyed")
        lifecycleInstance.destroyContext(context.get)
        context = None
        logger.info("Servlet context was destroyed")
      }
    }
  }

  private[this] lazy val config = Configuration.load()
  private[this] lazy val lifecycleInstance = {
    val cls = config.lifeCycleClass
    toInstance(cls) match {
      case Success(obj) => obj.asInstanceOf[LifeCycle]
      case Failure(ex) => throw ex
    }
  }

  private[this] def addFilterMapping(name: String, router: Filter, urlPattern: String, ctx: ServletContext) {
    val req = ctx.addFilter(name, router)
    req.setAsyncSupported(true)
    val dispatchers = jutil.EnumSet.allOf(classOf[DispatcherType])
    req.addMappingForUrlPatterns(dispatchers, true, urlPattern)
  }

  private[this] def addServletMapping(name: String, router: Servlet, urlPattern: String, ctx: ServletContext) {
    val req = ctx.addServlet(name, router)
    req.setAsyncSupported(true)
    req.addMapping(urlPattern)
  }

}
