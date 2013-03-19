package shifter.concurrency

import concurrent.{Await, ExecutionContext, Future}
import util.{Failure, Success, Try}
import concurrent.duration.{FiniteDuration, Duration}

trait FutureExtensions[A] extends Any {
  def future: Future[A]

  def await(implicit timeout: Duration): A =
    Await.result(future, timeout)

  def timeout(message: Try[A], exp: Duration)(implicit ec: ExecutionContext): Future[A] =
    exp match {
      case duration: FiniteDuration =>
        val timeoutPromise = Promise.timeout(message, duration)
        Future.firstCompletedOf(future :: timeoutPromise :: Nil)(ec)
      case _ =>
        future
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
