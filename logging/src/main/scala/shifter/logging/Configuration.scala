package shifter.logging

object Level extends Enumeration {
  val ALL, DEBUG, INFO, WARN, ERROR, OFF, TRACE, DEFAULT = Value
}

class LoggingConfiguration {
  /**
   * Format of the log lines.
   */
  var format = "%-5p %d{yyyy-MM-dd HH:mm:ss} %c{1}: %m\n"

  /**
   * Default logger level. Available options being:
   * ALL, DEBUG, INFO, WARN, ERROR, OFF, TRACE
   */
  var level = Level.INFO

  /**
   * Hijacks all logging handlers currently enabled by other libraries -
   * useful to start from scratch and impose the same configuration everywhere
   */
  var hijackJDKLogging = false

  /**
   * Settings for console logging.
   */
  val console = new ConsoleLoggingConfiguration
}

class ConsoleLoggingConfiguration {
  var enabled = true
  var stream = "System.out"
}
