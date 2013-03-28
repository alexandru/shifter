package shifter.io

import shifter.concurrency.extensions._
import java.nio.channels.{ClosedChannelException, CompletionHandler, AsynchronousFileChannel}
import collection.JavaConverters._
import java.nio.file.StandardOpenOption
import concurrent.{Future, Promise, ExecutionContext}
import java.nio.ByteBuffer
import java.io.File
import scala.util._
import control.NonFatal
import shifter.concurrency.atomic.Ref


class AsyncFileOutputStream(file: File)(implicit val ec: ExecutionContext)
    extends AsyncOutputStream {

  def write(bytes: ByteBuffer): Future[Int] = {
    val writePosition = signalJobStart(bytes)
    asyncWritePromise(bytes, writePosition).future
  }

  def flush(): Future[Unit] =
    try {
      instance.force(forceWithMetadata.getAndSet(false))
      Future.successful(())
    }
    catch {
      case NonFatal(ex) =>
        Future.failed(ex)
    }

  private[this] val writeCompletionHandler = new CompletionHandler[Integer, Promise[Int]] {
    def completed(result: Integer, promise: Promise[Int]) {
      promise.complete(Success(result))
      signalJobFinished(count = 1)
    }
    def failed(exc: Throwable, promise: Promise[Int]) {
      promise.complete(Failure(exc))
      signalJobFinished(count = 1)
    }
  }

  private[this] def asyncWritePromise[T](bytes: ByteBuffer, positionInFile: Long): Promise[Int] = {
    val promise = Promise[Int]()
    instance.write(bytes, positionInFile, promise, writeCompletionHandler)
    promise
  }

  def forceClose() {
    instance.close()
  }

  private[this] val lastPosition = {
    if (file.exists())
      Ref(file.length())
    else
      Ref(0L)
  }

  private[this] val options =
    Seq(StandardOpenOption.CREATE, StandardOpenOption.WRITE).toSet.asJava

  private[this] val instance =
    AsynchronousFileChannel.open(file.toPath, options, ec.toExecutorService)

  private[this] val forceWithMetadata = Ref(true)

  private[this] val isClosedRef = Ref(false)
  def isClosed: Boolean = isClosedRef()

  def asyncClose(): Future[Unit] = {
    val pending = synchronized {
      isClosedRef.set(true)
      pendingJobs.get
    }

    if (pending == 0) {
      val flushFuture = flush()
      flushFuture.onComplete { case _ => forceClose() }
      closingPromise.tryCompleteWith(flushFuture)
    }

    closingPromise.future
  }

  protected def signalJobStart(buffer: ByteBuffer): Long = {
    val result = synchronized {
      if (!isClosedRef()) {
        val pos = lastPosition.getAndTransform(_ + buffer.limit() - buffer.position())
        pendingJobs.transform(_ + 1)
        Some(pos)
      }
      else
        None
    }

    if (result.isDefined)
      result.get
    else
      throw new ClosedChannelException
  }

  private[this] def signalJobFinished(count: Int = 1) {
    val shouldClose = synchronized {
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
