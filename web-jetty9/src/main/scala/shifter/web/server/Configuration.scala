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
   * If None, then a QueuedThreadPool is used, which has no upper bound.
   * If Some(Int), then the thread pool created will use a bounded LinkedBlockingQueue,
   * useful for limiting the number of incoming connections.
   */
  threadPoolMaxQueueSize: Option[Int] = None,

  /**
   * Set the maximum thread idle time.
   * Threads that are idle for longer than this period may be
   * stopped.
   */
  threadPoolIdleTimeout: Int = 60000,

  /**
   * Number of connection requests that can be queued up before the operating system
   * starts to send rejections.
   */
  acceptQueueSize: Int = 0,

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
  idleTimeoutMillis: Int = 10000,

  /**
   * The linger time. Use -1 to disable.
   *
   * See:
   *   http://stackoverflow.com/a/13088864/3280
   *   http://www.serverframework.com/asynchronousevents/2011/01/time-wait-and-its-design-implications-for-protocols-and-scalable-servers.html
   */
  soLingerTime: Int = -1
)

object Configuration {
  def load(): Configuration = {
    val values = ConfigFactory.load().withFallback(
      ConfigFactory.load("shifter/web/server/reference.conf")
    )

    val lifeCycleClass = toClass(values.getString("http.server.lifeCycleClass"))
      .getOrElse(classOf[DefaultLifeCycle])

    if (!isSubclass[LifeCycle](lifeCycleClass))
      throw new BadValue("http.server.lifeCycleClass", "Value is not a valid LifeCycle class")

    val defaultConfig: Configuration = Configuration(
      host = values.getString("http.server.host"),
      port = values.getInt("http.server.port"),
      resourceBase = values.getString("http.server.resourceBase"),
      lifeCycleClass = lifeCycleClass,
      error404 = values.getString("http.server.error404"),
      error500 = values.getString("http.server.error500"),
      error403 = values.getString("http.server.error403")
    )

    val numberOfProcessors = math.max(Runtime.getRuntime.availableProcessors(), 1)
    val parallelismFactor = Try(values.getInt("http.server.parallelismFactor")).getOrElse(defaultConfig.parallelismFactor)
    val parallelism = numberOfProcessors * parallelismFactor

    val declaredMinThreads = Try(values.getInt("http.server.minThreads")).getOrElse(defaultConfig.minThreads)
    val declaredMaxThreads = Try(values.getInt("http.server.maxThreads")).getOrElse(defaultConfig.maxThreads)

    val minThreads = math.max(1, declaredMinThreads)
    val maxThreads = math.max(declaredMinThreads, math.min(declaredMaxThreads, parallelism))

    defaultConfig.copy(
      minThreads = minThreads,
      maxThreads = maxThreads,
      parallelismFactor = parallelismFactor,
      threadPoolMaxQueueSize = Try(values.getInt("http.server.threadPoolMaxQueueSize")).toOption,
      threadPoolIdleTimeout = Try(values.getInt("http.server.threadPoolIdleTimeout")).getOrElse(defaultConfig.threadPoolIdleTimeout),
      acceptQueueSize = Try(values.getInt("http.server.acceptQueueSize")).getOrElse(defaultConfig.acceptQueueSize),
      idleTimeoutMillis = Try(values.getInt("http.server.idleTimeoutMillis")).getOrElse(defaultConfig.idleTimeoutMillis),
      soLingerTime = Try(values.getInt("http.server.soLingerTime")).getOrElse(defaultConfig.soLingerTime)
    )
  }
}