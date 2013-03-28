package shifter.io

import concurrent.{ExecutionContext, Promise, Future}
import concurrent.stm._
import java.nio.channels.ClosedChannelException


trait AbstractAsyncOutputStream extends AsyncOutputStream {
  implicit protected val ec: ExecutionContext

  protected val isClosedRef = Ref(initialValue = false)
  def isClosed: Boolean = isClosedRef.single.get

  def asyncClose(): Future[Unit] = {
    val pending = atomic { implicit txn =>
      isClosedRef() = true
      pendingJobs()
    }

    if (pending == 0) {
      val flushFuture = flush()
      flushFuture.onComplete { case _ => forceClose() }
      closingPromise.tryCompleteWith(flushFuture)
    }

    closingPromise.future
  }

  protected def signalJobStart[T](cb: InTxn => T): T = {
    val result = atomic{ implicit txn =>
      if (!isClosedRef()) {
        val result = cb(txn)
        pendingJobs.transform(_ + 1)
        Some(result)
      }
      else
        None
    }

    if (result.isDefined)
      result.get
    else
      throw new ClosedChannelException
  }

  protected def signalJobFinished(count: Int = 1) {
    val shouldClose = atomic { implicit txn =>
      val pending = pendingJobs.transformAndGet(x => math.max(x - count, 0))

      if (pending == 0)
        isClosedRef()
      else
        false
    }

    if (shouldClose) {
      val flushFuture = flush()
      flushFuture.onComplete { case _ => forceClose() }
      closingPromise.completeWith(flushFuture)
    }
  }

  private[this] val pendingJobs = Ref(0L)
  private[this] val closingPromise = Promise[Unit]()

}
