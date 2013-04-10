package shifter.io

import java.util.concurrent.{ForkJoinWorkerThread, ForkJoinPool}
import scala.concurrent.ExecutionContext
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory

object Implicits {
  implicit lazy val IOContext: ExecutionContext = {
    val factory = new ForkJoinWorkerThreadFactory {
      private[this] val default = ForkJoinPool.defaultForkJoinWorkerThreadFactory

      def newThread(pool: ForkJoinPool): ForkJoinWorkerThread = {
        val th = default.newThread(pool)
        th.setDaemon(true)
        th.setName("shifter-io")
        th
      }
    }

    val executor = new ForkJoinPool(1, factory, null, true)
    ExecutionContext.fromExecutor(executor)
  }
}
