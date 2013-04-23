package shifter.concurrency.extensions

import concurrent.ExecutionContext
import java.util.concurrent.Executor
import util.control.NonFatal

/**
 * Wrapper for transforming an `ExecutionContext` into a Java
 * [[http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Executor.html Executor]]
 */
class ExecutorWrapper(ec: ExecutionContext) extends Executor {
  def execute(command: Runnable) {
    try ec.execute(command) catch { case NonFatal(ex) => ec.reportFailure(ex) }
  }
}
