package shifter.concurrency

import concurrent.{Await, ExecutionContext, Future}
import scala.util.Try
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
}
