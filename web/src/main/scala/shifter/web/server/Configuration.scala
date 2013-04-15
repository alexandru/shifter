package shifter.web.server

import language.existentials
import com.typesafe.config.ConfigFactory
import shifter.reflection.{toClass, isSubclass}
import com.typesafe.config.ConfigException.BadValue
import util.Try

case class Configuration(
  host: String,
  port: Int,
  minThreads: Int,
  maxThreads: Int,
  parallelismFactor: Int,
  resourceBase: String,
  lifeCycleClass: Class[_],
  error404: String,
  error500: String,
  error403: String,
  killOnFatalError: Boolean,
  isInstrumented: Boolean,
  metrics: MetricsConfiguration
)

case class MetricsConfiguration(
  mapping: String,
  realm: String,
  username: String,
  password: String,
  graphiteEnabled: Boolean,
  graphiteServerHost: Option[String],
  graphiteServerPort: Option[Int],
  graphiteServerPrefix: Option[String]
)


object Configuration {
  def load() = {
    val values = ConfigFactory.load().withFallback(
      ConfigFactory.load("shifter/web/server/reference.conf")
    )

    val lifeCycleClass = toClass(values.getString("http.server.lifeCycleClass"))
      .getOrElse(classOf[DefaultLifeCycle])

    if (!isSubclass[LifeCycle](lifeCycleClass))
      throw new BadValue("http.server.lifeCycleClass", "Value is not a valid LifeCycle class")

    val numberOfProcessors = math.max(Runtime.getRuntime.availableProcessors(), 1)
    val parallelismFactor = Try(values.getInt("http.server.parallelismFactor")).getOrElse(2)
    val parallelism = numberOfProcessors * parallelismFactor

    val declaredMinThreads = Try(values.getInt("http.server.minThreads")).getOrElse(numberOfProcessors)
    val declaredMaxThreads = Try(values.getInt("http.server.maxThreads")).getOrElse(parallelism)

    val minThreads = math.max(1, declaredMinThreads)
    val maxThreads = math.max(declaredMinThreads, math.min(declaredMaxThreads, parallelism))

    Configuration(
      host = values.getString("http.server.host"),
      port = values.getInt("http.server.port"),
      parallelismFactor = parallelismFactor,
      minThreads = minThreads,
      maxThreads = maxThreads,
      resourceBase = values.getString("http.server.resourceBase"),
      lifeCycleClass = lifeCycleClass,
      error404 = values.getString("http.server.error404"),
      error500 = values.getString("http.server.error500"),
      error403 = values.getString("http.server.error403"),
      killOnFatalError = values.getBoolean("http.server.killOnFatalError"),
      isInstrumented = values.getBoolean("http.server.isInstrumented"),
      metrics = MetricsConfiguration(
        mapping = values.getString("http.server.metrics.mapping"),
        realm = values.getString("http.server.metrics.realm"),
        username = values.getString("http.server.metrics.username"),
        password = values.getString("http.server.metrics.password"),
        graphiteEnabled = Try(values.getBoolean("http.server.metrics.graphiteEnabled")).getOrElse(false),
        graphiteServerHost = Try(values.getString("http.server.metrics.graphiteServerHost")).toOption,
        graphiteServerPort = Try(values.getInt("http.server.metrics.graphiteServerPort")).toOption,
        graphiteServerPrefix = Try(values.getString("http.server.metrics.graphiteServerPrefix")).toOption
      )
    )
  }
}