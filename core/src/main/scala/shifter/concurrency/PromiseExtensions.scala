package shifter.concurrency

import concurrent.{ExecutionContext, Future, Promise}
import concurrent.duration.FiniteDuration
import util.Try

trait PromiseExtensions[A] extends Any {
  val promise: Promise[A]

  def futureWithTimeout(exp: FiniteDuration)(cb: => Try[A])(implicit ec: ExecutionContext): Future[A] = {
    val timeoutTask = scheduler.runOnce(exp.toMillis) {
      promise.tryComplete(cb)
    }

    val future = promise.future
    future.onComplete {
      case _ =>
        scheduler.cancel(timeoutTask)
    }

    future
  }

  def futureWithTimeout(message: Try[A], exp: FiniteDuration)(implicit ec: ExecutionContext): Future[A] = {
    val timeoutTask = scheduler.runOnce(exp.toMillis) {
      promise.tryComplete(message)
    }

    val future = promise.future
    future.onComplete {
      case _ =>
        scheduler.cancel(timeoutTask)
    }

    future
  }
}
