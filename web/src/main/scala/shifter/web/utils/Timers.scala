package shifter.web.utils

import concurrent.{ExecutionContext, Future}
import com.yammer.metrics.Metrics
import java.util.concurrent.TimeUnit
import com.yammer.metrics.core.Timer
import concurrent.stm._
import shifter.web.server.Configuration


trait Timers {
  def measured[T](name: String)(cb: => T): T =
    getTimer(name).map { timer =>
      val ctx = timer.time()
      try {
        cb
      }
      finally {
        ctx.stop()
      }
    }
    .getOrElse(cb)

  def measuredFuture[T](name: String)(cb: => Future[T])(implicit ec: ExecutionContext): Future[T] =
    getTimer(name).map { timer =>
      val ctx = timer.time()
      val value = cb
      value.onComplete(_ => ctx.stop())
      value
    }
    .getOrElse(cb)

  private[this] def getTimer(name: String): Option[Timer] = {
    if (!httpConfig.isInstrumented)
      None
    else {
      val tm = timers.single.transformAndGet { map =>
        map.get(name) match {
          case Some(_) => map
          case None =>
            val newTimer = Metrics.newTimer(
              this.getClass,
              name,
              TimeUnit.MILLISECONDS,
              TimeUnit.SECONDS
            )

            map.updated(name, newTimer)
        }
      }

      Some(tm(name))
    }
  }

  private[this] val timers = Ref(Map.empty[String, Timer])
  private[this] lazy val httpConfig = Configuration.load()
}
