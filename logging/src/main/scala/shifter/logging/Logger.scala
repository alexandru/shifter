package shifter.logging


import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import ch.qos.logback.classic.{PatternLayout, Level => LLevel, Logger => SLF4JLogger}
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.encoder.LayoutWrappingEncoder


object Logger {
  def apply(name: String) = 
    LoggerFactory.getLogger(name)

  def apply(cls: Class[_]) =
    LoggerFactory.getLogger(cls)

  def configure(f: LoggingConfiguration => Unit) = {
    val config = new LoggingConfiguration
    f(config)
    initializeWith(config)
  }

  def configureWithDefaults() =
    initializeWith(new LoggingConfiguration)

  private[this] def initializeWith(config: LoggingConfiguration) {
    this.synchronized {
      val root = bootstrapRoot
      hijackJDKLogging

      val defaultLevel = toSl4jLevel(config.level, None)
      root.setLevel(defaultLevel)

      val context = root.getLoggerContext()
      val layout = new PatternLayout()
      layout.setOutputPatternAsHeader(false)
      layout.setPattern(config.format)
      layout.setContext(context)
      layout.start()

      if (config.console.enabled) {
        val appender = new ConsoleAppender[ILoggingEvent]();
        appender.setTarget(config.console.stream)
        appender.setContext(context)
        appender.setLayout(layout)
        (appender.getEncoder.asInstanceOf[LayoutWrappingEncoder[_]])
          .setImmediateFlush(false)
        appender.start()

        root.addAppender(appender)
      }
    }
  }

  private[this] def toSl4jLevel(level: Level.Value, default: Option[LLevel]) = 
    level match {
      case Level.ALL => LLevel.ALL
      case Level.DEBUG => LLevel.DEBUG
      case Level.ERROR => LLevel.ERROR
      case Level.OFF => LLevel.OFF
      case Level.WARN => LLevel.WARN
      case Level.TRACE => LLevel.TRACE
      case _ => default.getOrElse(LLevel.INFO)
    }

  private[this] def bootstrapRoot = {
    val root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
      .asInstanceOf[SLF4JLogger]
    root.detachAndStopAllAppenders()
    root
  }

  private[this] def hijackJDKLogging = {
    val root: java.util.logging.Logger = java.util.logging.Logger.getLogger("")
    for (handler <- root.getHandlers)
      root.removeHandler(handler)
    root.addHandler(new SLF4JBridgeHandler())
  }
}
