package shifter.io

import java.util.concurrent.{ForkJoinWorkerThread, ForkJoinPool}
import scala.concurrent.ExecutionContext
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory
import shifter.concurrency.atomic.Ref

object Implicits {
  implicit lazy val IOContext: ExecutionContext = {
    val factory = new ForkJoinWorkerThreadFactory {
      private[this] val counter = Ref(0L)
      private[this] def newName() =
        "shifter-io-" + counter.transformAndGet(c => if (c + 1 > 0) c + 1 else 1).toString
      private[this] val default = ForkJoinPool.defaultForkJoinWorkerThreadFactory

      def newThread(pool: ForkJoinPool): ForkJoinWorkerThread = {
        val th = default.newThread(pool)
        th.setDaemon(true)
        th.setName(newName())
        th
      }
    }

    val executor = new ForkJoinPool(2, factory, null, true)
    ExecutionContext.fromExecutor(executor)
  }
}
