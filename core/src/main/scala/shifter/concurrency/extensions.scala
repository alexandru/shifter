package shifter.concurrency

import concurrent._
import internals.{ExecutorServiceWrapper, ExecutorWrapper, PromiseExtensions, FutureExtensions}
import java.util.concurrent.{Executor, ExecutorService}

object extensions {
  implicit class FutureExtensionsImplicit[A](val future: Future[A])
    extends AnyVal with FutureExtensions[A]

  implicit class PromiseExtensionsImplicit[A](val promise: Promise[A])
    extends AnyVal with PromiseExtensions[A]

  implicit class ExecutionContextExtensions(val ec: ExecutionContext) extends AnyVal {
    def toExecutor: Executor = ec match {
      case null => throw null
      case executor: ExecutionContextExecutorService => executor
      case executor: ExecutionContextExecutor => executor
      case _ => new ExecutorWrapper(ec)
    }

    def toExecutorService: ExecutorService = ec match {
      case null => throw null
      case executor: ExecutionContextExecutorService => executor
      case _ => new ExecutorServiceWrapper(ec)
    }
  }
}
