package shifter.concurrency

import scala.concurrent._
import java.util.concurrent.{ExecutorService, Executor}

/**
 * Useful extensions for various concurrency primitives.
 */
package object extensions {
  /**
   * Extensions for
   * [[http://www.scala-lang.org/api/current/index.html#scala.concurrent.ExecutionContext concurrent.ExecutionContext]]
   */
  implicit class ExecutionContextExtensions(val ec: ExecutionContext) extends AnyVal {
    /**
     * Transforms this Scala `ExecutionContext` into a Java
     * [[http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Executor.html Executor]]
     */
    def toExecutor: Executor = ec match {
      case null => throw null
      case executor: ExecutionContextExecutorService => executor
      case executor: ExecutionContextExecutor => executor
      case _ => new ExecutorWrapper(ec)
    }

    /**
     * Transforms this Scala `ExecutionContext` into a Java
     * [[http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html ExecutorService]]
     */
    def toExecutorService: ExecutorService = ec match {
      case null => throw null
      case executor: ExecutionContextExecutorService => executor
      case _ => new ExecutorServiceWrapper(ec)
    }
  }
}
