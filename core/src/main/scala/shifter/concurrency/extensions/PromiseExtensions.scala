package shifter.concurrency.extensions

import concurrent.{ExecutionContext, Future, Promise}
import concurrent.duration.FiniteDuration
import util.Try
import shifter.concurrency.scheduler

/**
 * [[http://www.scala-lang.org/api/current/index.html#scala.concurrent.Promise concurrent.Promise]]
 * extensions for greater joy.
 */
trait PromiseExtensions[A] extends Any {
  val promise: Promise[A]

  /**
   * Completes this `promise` after a specified timeout duration, in case the
   * promise isn't already ''complete'' with the specified result.
   *
   * @example {{{
   *   val promise = Promise[String]()
   *
   *   val future = promise.futureWithTimeout(2.seconds) {
   *     Failed(new concurrent.TimeoutException("2 seconds"))
   *   }
   * }}}
   */
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

  /**
   * Completes this `promise` after a specified timeout duration, in case the
   * promise isn't already ''complete'' with the specified result.
   *
   * @example {{{
   *   val promise = Promise[String]()
   *
   *   val onTimeout = Failed(new concurrent.TimeoutException("2 seconds"))
   *   val future = promise.futureWithTimeout(onTimeout, 2.seconds)
   * }}}
   */
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
