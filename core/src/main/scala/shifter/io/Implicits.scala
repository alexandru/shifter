package shifter.io

import java.util.concurrent.{ForkJoinWorkerThread, ForkJoinPool}
import scala.concurrent.ExecutionContext
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory

object Implicits {
  /**
   * A thread-pool used for dealing with I/O, of size 1.
   *
   * Having a thread-pool for I/O that's separate from the application's
   * default thread-pool is recommended, as otherwise under load you can
   * end up with really nasty dead-locks.
   *
   * Currently used by [[shifter.io.AsyncFileChannel]] and by `shifter-cache`.
   */
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
