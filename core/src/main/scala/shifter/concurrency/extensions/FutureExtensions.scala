package shifter.concurrency.extensions

import concurrent.{Promise, Await, ExecutionContext, Future}
import util.{Failure, Success, Try}
import concurrent.duration.{FiniteDuration, Duration}
import shifter.concurrency.scheduler

/**
 * [[http://www.scala-lang.org/api/current/index.html#scala.concurrent.Future concurrent.Future]]
 * extensions for greater joy.
 */
trait FutureExtensions[A] extends Any {
  val future: Future[A]

  /**
   * Shortcut for `scala.concurrent.Await.result`
   */
  def await(implicit timeout: Duration): A =
    Await.result(future, timeout)

  /**
   * Specifies a timeout for completing this `future`.
   *
   * If the specified expiration time elapses, then the future
   * is completed with the specified result.
   *
   * @example {{{
   *   val future: Future[String] = Future {
   *     Thread.sleep(2000)
   *     "Slept for 2 seconds"
   *   }
   *
   *   val withTimeout = future.timeout(1.second) {
   *     Success("Timeout after 1 second triggered")
   *   }
   * }}}
   *
   * @param exp - the expiration time
   * @param cb - the completion value on timeout
   * @param ec - the implicit execution context used for executing `cb`
   * @return - a new `Future`
   */
  def timeout(exp: FiniteDuration)(cb: => Try[A])(implicit ec: ExecutionContext): Future[A] = {
    val timeoutPromise = Promise[A]()
    val timeoutTask = scheduler.runOnce(exp.toMillis) {
      timeoutPromise.tryComplete(cb)
    }

    future.onComplete {
      case _ =>
        scheduler.cancel(timeoutTask)
    }

    Future.firstCompletedOf(Seq(future, timeoutPromise.future))
  }

  /**
   * Creates a new future by applying a function to the successful
   * result of this future.
   *
   * The difference between this function and the normal
   * `map` function is that this variant executes the actual mapping
   * immediately in the current thread if the future is already complete.
   */
  def lightMap[S](f: A => S)(implicit ec: ExecutionContext): Future[S] =
    if (future.isCompleted)
      future.value.get match {
        case Success(value) =>
          Try(f(value)) match {
            case Success(result) =>
              Future.successful(result)
            case Failure(ex) =>
              Future.failed(ex)
          }
        case Failure(ex) =>
          Future.failed(ex)
      }
    else
      future.map(f)

  /**
   * Creates a new future by applying a function to the successful
   * result of this future of a `map` followed by a `flatten`.
   *
   * The difference between this function and the normal
   * `flatMap` function is that this variant executes the actual mapping
   * immediately in the current thread if the future is already complete.
   */
  def lightFlatMap[S](f: A => Future[S])(implicit ec: ExecutionContext): Future[S] =
    if (future.isCompleted)
      future.value.get match {
        case Success(value) =>
          f(value)
        case Failure(ex) =>
          Future.failed(ex)
      }
    else
      future.flatMap(f)
}
