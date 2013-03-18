package shifter.concurrency

import java.util.concurrent.TimeUnit
import concurrent.{Promise => ScalaPromise, Future, ExecutionContext}
import concurrent.duration.FiniteDuration
import util.Try

object Promise {
  def timeout[A](value: Try[A], duration: FiniteDuration): Future[A] =
    timeout(value, duration.toMillis, TimeUnit.MILLISECONDS)

  def timeout[A](value: Try[A], duration: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Future[A] = {
    val p = ScalaPromise[A]()
    timer.schedule( new java.util.TimerTask{
      def run(){
        p.complete(value)
      }
    },unit.toMillis(duration) )
    p.future
  }

  def timeout[A](duration: FiniteDuration)(cb: => A)(implicit ec: ExecutionContext): Future[A] = {
    val p = ScalaPromise[A]()
    timer.schedule( new java.util.TimerTask{
      def run(){
        p.completeWith(Future(cb)(ec))
      }
    },duration.toMillis )
    p.future
  }

  private[this] val timer = new java.util.Timer()
}
