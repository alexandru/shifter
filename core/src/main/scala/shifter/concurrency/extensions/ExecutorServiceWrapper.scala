package shifter.concurrency.extensions

import concurrent._
import java.util.concurrent.{AbstractExecutorService, TimeUnit}
import java.util.Collections


/**
 * Wrapper for transforming an `ExecutionContext` into a Java
 * [[http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html ExecutorService]]
 */
class ExecutorServiceWrapper protected[concurrency] (ec: ExecutionContext)
    extends AbstractExecutorService with ExecutionContextExecutorService {

  override def prepare(): ExecutionContext = ec
  override def isShutdown = false
  override def isTerminated = false
  override def shutdown() = ()
  override def shutdownNow() = Collections.emptyList[Runnable]
  override def execute(runnable: Runnable): Unit = ec execute runnable
  override def reportFailure(t: Throwable): Unit = ec reportFailure t
  override def awaitTermination(length: Long,unit: TimeUnit): Boolean = false
}
