package shifter.web.server

import com.typesafe.scalalogging.slf4j.Logging
import org.eclipse.jetty.server.Server
import com.yammer.metrics.jetty.{InstrumentedQueuedThreadPool, InstrumentedSelectChannelConnector}
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.webapp.WebAppContext
import java.io._
import scala.Some
import com.yammer.metrics.reporting.GraphiteReporter
import java.util.concurrent.TimeUnit


object Jetty extends Logging {

  def start(): Server = {
    start(Configuration.load())
  }
  
  def start(config: Configuration): Server = {

    if (config.metrics.graphiteEnabled)
      for (gHost <- config.metrics.graphiteServerHost;
           gPort <- config.metrics.graphiteServerPort;
           gPrefix <- config.metrics.graphiteServerPrefix) {
        logger.info("Starting Graphite reporting")
        GraphiteReporter.enable(1, TimeUnit.MINUTES, gHost, gPort, gPrefix)
      }

    logger.info(
      "Starting Jetty on %s:%d with minThreads=%d, maxThreads=%d, killOnFatalError=%s and isInstrumented=%s".format(
          config.host, config.port, config.minThreads, config.maxThreads,
          config.killOnFatalError, config.isInstrumented
        ))

    val server = new Server()

    val connector = if (config.isInstrumented)
      new InstrumentedSelectChannelConnector(config.port)
    else
      new SelectChannelConnector()

    connector.setPort(config.port)
    connector.setHost(config.host)
    server.addConnector(connector)

    val pool = if (config.isInstrumented)
      new InstrumentedQueuedThreadPool()
    else
      new QueuedThreadPool()

    pool.setMinThreads(config.minThreads)
    pool.setMaxThreads(config.maxThreads)
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
    val resource = if (config.isInstrumented)
      Option(getClass.getResource("/shifter/web/server/web.xml"))
    else
      Option(getClass.getResource("/shifter/web/server/web-plain.xml"))

    val webXmlTemplate = resource match {
      case None =>
        throw new RuntimeException("Cannot find resource: /shifter/web/server/web.xml")

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
      .replace("$metrics-mapping", config.metrics.mapping)

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
