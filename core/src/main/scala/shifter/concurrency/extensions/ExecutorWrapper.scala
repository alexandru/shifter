package shifter.concurrency.extensions

import concurrent.ExecutionContext
import java.util.concurrent.Executor
import util.control.NonFatal

class ExecutorWrapper(ec: ExecutionContext) extends Executor {
  def execute(command: Runnable) {
    try ec.execute(command) catch { case NonFatal(ex) => ec.reportFailure(ex) }
  }
}
