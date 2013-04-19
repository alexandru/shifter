package shifter.web.jetty9

import com.typesafe.scalalogging.slf4j.Logging
import org.eclipse.jetty.server.{HttpConnectionFactory, Server, ServerConnector}
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.webapp.WebAppContext
import java.io._
import java.util.concurrent.LinkedBlockingQueue
import shifter.reflection._
import scala.Some

trait Jetty extends Logging {
  def start(): Server = {
    start(Configuration.load())
  }

  def start(lifeCycleClass: Class[_]): Server = {
    if (!isSubclass[LifeCycle](lifeCycleClass))
      throw new IllegalArgumentException("Value is not a valid LifeCycle class")

    start(Configuration.load(lifeCycleClass))
  }

  def start(config: Configuration): Server = {
    logger.info("Starting Jetty on %s:%d".format(config.host, config.port))

    logger.info(s"Jetty config.parallelismFactor: ${config.parallelismFactor}")
    logger.info(s"Jetty config.minThreads: ${config.minThreads}")
    logger.info(s"Jetty config.maxThreads: ${config.maxThreads}")
    logger.info(s"Jetty config.threadPoolMaxQueueSize: ${config.threadPoolMaxQueueSize}")
    logger.info(s"Jetty config.threadPoolIdleTimeout: ${config.threadPoolIdleTimeout}")

    val pool = if (config.threadPoolMaxQueueSize.isDefined) {
      val maxQueueSize = config.threadPoolMaxQueueSize.get
      val queue = new LinkedBlockingQueue[Runnable](maxQueueSize)
      new QueuedThreadPool(config.maxThreads, config.minThreads, config.threadPoolIdleTimeout, queue)
    }
    else
      new QueuedThreadPool(config.maxThreads, config.minThreads, config.threadPoolIdleTimeout)

    val server = new Server(pool)

    logger.info(s"Jetty config.acceptors: ${config.acceptors}")
    logger.info(s"Jetty config.selectors: ${config.selectors}")

    val connector = new ServerConnector(server, null,null,null,config.acceptors,config.selectors,new HttpConnectionFactory())

    connector.setHost(config.host)
    connector.setPort(config.port)

    logger.info(s"Jetty config.acceptQueueSize: ${config.acceptQueueSize}")
    connector.setAcceptQueueSize(config.acceptQueueSize)

    logger.info(s"Jetty config.soLingerTime: ${config.soLingerTime}")
    connector.setSoLingerTime(config.soLingerTime)

    logger.info(s"Jetty config.maxIdleTime: ${config.idleTimeoutMillis}")
    connector.setIdleTimeout(config.idleTimeoutMillis)

    server.addConnector(connector)

    val resourceBase = Option(getClass.getResource(config.resourceBase)) match {
      case Some(res) => res.toExternalForm
      case None => throw new RuntimeException("Couldn't find resource base: " + config.resourceBase)
    }

    val webXmlPath = getWebXmlPath(config)

    val context = new WebAppContext()
    context.setDescriptor(webXmlPath)
    context.setResourceBase(resourceBase)
    context.setContextPath("/")
    context.setParentLoaderPriority(true)

    server.setStopAtShutdown(true)
    server.setHandler(context)
    server.start()
    server
  }

  def run() {
    start().join()
  }

  def run(lifeCycleClass: Class[_]) {
    start(lifeCycleClass).join()
  }

  def run(config: Configuration) {
    start(config).join()
  }

  def getWebXmlPath(config: Configuration): String = {
    val resource =
      Option(getClass.getResource("/shifter/web/jetty9/web-plain.xml"))

    val webXmlTemplate = resource match {
      case None =>
        throw new RuntimeException("Cannot find resource: /shifter/web/jetty9/web-plain.xml")

      case Some(res) =>
        val in = new BufferedReader(new InputStreamReader(res.openStream(), "UTF-8"))
        try {
          val content = new StringBuilder
          var line: Option[String] = None

          do {
            line = Option(in.readLine())
            if (line.isDefined)
              content.append(line.get).append("\n")
          } while (line.isDefined)
          content.toString()
        }
        finally {
          in.close()
        }
    }

    val webXmlContent = webXmlTemplate
      .replace("$error-404", config.error404)
      .replace("$error-500", config.error500)
      .replace("$error-403", config.error403)

    val file = File.createTempFile("web.", ".xml")
    file.deleteOnExit()

    val out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))
    try {
      out.write(webXmlContent)
    }
    finally {
      out.close()
    }

    file.getAbsolutePath
  }
}

object Jetty extends Jetty