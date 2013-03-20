package shifter.concurrency

import concurrent.{Promise, Await, ExecutionContext, Future}
import util.{Failure, Success, Try}
import concurrent.duration.{FiniteDuration, Duration}

trait FutureExtensions[A] extends Any {
  val future: Future[A]

  def await(implicit timeout: Duration): A =
    Await.result(future, timeout)

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
