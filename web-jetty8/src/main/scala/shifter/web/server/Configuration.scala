package shifter.web.server

import language.existentials
import com.typesafe.config.ConfigFactory
import shifter.reflection.{toClass, isSubclass}
import com.typesafe.config.ConfigException.BadValue
import util.Try

case class Configuration(
  host: String,
  port: Int,

  resourceBase: String,
  lifeCycleClass: Class[_],
  error404: String,
  error500: String,
  error403: String,

  /**
   * Minimum number of application threads that process the actual HTTP responses.
   */
  minThreads: Int = 1,

  /**
   * Upper bound for the application threads that process the actual HTTP responses.
   */
  maxThreads: Int = 26,

  /**
   * Used to calculate the number of threads that process the actual HTTP responses,
   * based on the number of CPUs available, using as lower and upper bounds the
   * minThreads and maxThreads settings.
   */
  parallelismFactor: Int = 2,

  /**
   * Number of acceptor threads to use.
   */
  acceptors: Int = 1,

  /**
   * Number of connection requests that can be queued up before the operating system
   * starts to send rejections. Useful for limiting the total number of connections
   * that can be accepted.
   */
  acceptQueueSize: Int = 0,

  /**
   * Sets the number of connections, which if exceeded places this connector in a low resources
   * state. This is not an exact measure as the connection count is averaged over the select sets.
   * When in a low resources state, different idle timeouts can apply on connections.
   * See lowResourcesMaxIdleTime.
   */
  lowResourcesConnections: Int = 0,

  /**
   * Sets the period in ms that a connection is allowed to be idle when this there are more than
   * lowResourcesConnections connections. This allows the server to rapidly close idle connections
   * in order to gracefully handle high load situations.
   */
  lowResourcesMaxIdleTime: Int = 0,

  /**
   * Set the maximum Idle time for a connection, which roughly translates to the Socket.setSoTimeout(int)
   * call, although with NIO implementations other mechanisms may be used to implement the timeout.
   * The max idle time is applied: when waiting for a new request to be received on a connection;
   * when reading the headers and content of a request; when writing the headers and content of a response.
   * Jetty interprets this value as the maximum time between some progress being made on the connection.
   * So if a single byte is read or written, then the timeout (if implemented by Jetty) is reset. However,
   * in many instances, the reading/writing is delegated to the JVM, and the semantic is more strictly
   * enforced as the maximum time a single read/write operation can take. Note that as Jetty supports
   * writes of memory-mapped file buffers, a write may take many 10s of seconds for large content written to
   * a slow device.
   */
  maxIdleTime: Int = 10000
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

    val acceptors = Try(values.getInt("http.server.acceptors")).getOrElse(numberOfProcessors)
    val minThreads = math.max(1, declaredMinThreads) + acceptors
    val maxThreads = math.max(declaredMinThreads, math.min(declaredMaxThreads, parallelism)) + acceptors

    Configuration(
      host = values.getString("http.server.host"),
      port = values.getInt("http.server.port"),
      parallelismFactor = parallelismFactor,
      minThreads = minThreads,
      maxThreads = maxThreads,
      acceptors = acceptors,
      resourceBase = values.getString("http.server.resourceBase"),
      lifeCycleClass = lifeCycleClass,
      error404 = values.getString("http.server.error404"),
      error500 = values.getString("http.server.error500"),
      error403 = values.getString("http.server.error403")
    )
  }
}