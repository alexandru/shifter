package shifter.web.server

import com.typesafe.scalalogging.slf4j.Logging
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.server.nio.SelectChannelConnector
import java.io._
import java.util.concurrent.LinkedBlockingQueue

trait Jetty extends Logging {
  def start(): Server = {
    start(Configuration.load())
  }
  
  def start(config: Configuration): Server = {
    logger.info("Starting Jetty on %s:%d".format(config.host, config.port))

    val server = new Server()
    val connector = new SelectChannelConnector()

    logger.info(s"Jetty config.host: ${config.host}")
    connector.setHost(config.host)

    logger.info(s"Jetty config.port: ${config.port}")
    connector.setPort(config.port)

    logger.info(s"Jetty config.acceptors: ${config.acceptors}")
    connector.setAcceptors(config.acceptors)

    logger.info(s"Jetty config.acceptQueueSize: ${config.acceptQueueSize}")
    connector.setAcceptQueueSize(config.acceptQueueSize)

    logger.info(s"Jetty config.lowResourcesConnections: ${config.lowResourcesConnections}")
    connector.setLowResourcesConnections(config.lowResourcesConnections)

    logger.info(s"Jetty config.lowResourcesMaxIdleTime: ${config.lowResourcesMaxIdleTime}")
    connector.setLowResourcesMaxIdleTime(config.lowResourcesMaxIdleTime)

    logger.info(s"Jetty config.idleTimeoutMillis: ${config.idleTimeoutMillis}")
    connector.setMaxIdleTime(config.idleTimeoutMillis)

    logger.info(s"Jetty config.soLingerTime: ${config.soLingerTime}")
    connector.setSoLingerTime(config.soLingerTime)

    server.addConnector(connector)

    val pool = if (config.threadPoolMaxQueueSize.isDefined) {
      val maxQueueSize = config.threadPoolMaxQueueSize.get
      val queue = new LinkedBlockingQueue[Runnable](maxQueueSize)
      new QueuedThreadPool(queue)
    }
    else
      new QueuedThreadPool()

    logger.info(s"Jetty config.parallelismFactor: ${config.parallelismFactor}")

    logger.info(s"Jetty config.minThreads: ${config.minThreads}")
    pool.setMinThreads(config.minThreads)

    logger.info(s"Jetty config.maxThreads: ${config.maxThreads}")
    pool.setMaxThreads(config.maxThreads)

    logger.info(s"Jetty config.threadPoolMaxQueueSize: ${config.maxThreads}")
    if (config.threadPoolMaxQueueSize.isDefined)
      pool.setMaxQueued(config.threadPoolMaxQueueSize.get)

    logger.info(s"Jetty config.threadPoolIdleTimeout: ${config.threadPoolIdleTimeout}")
    pool.setMaxIdleTimeMs(config.threadPoolIdleTimeout)

    server.setThreadPool(pool)

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

  def run(config: Configuration) {
    start(config).join()
  }

  def getWebXmlPath(config: Configuration): String = {
    val resource =
      Option(getClass.getResource("/shifter/web/server/web-plain.xml"))

    val webXmlTemplate = resource match {
      case None =>
        throw new RuntimeException("Cannot find resource: /shifter/web/server/web-plain.xml")

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