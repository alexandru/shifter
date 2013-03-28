package shifter.io

import shifter.concurrency.extensions._
import java.nio.channels.{ClosedChannelException, CompletionHandler, AsynchronousFileChannel}
import collection.JavaConverters._
import java.nio.file.StandardOpenOption
import concurrent.{Future, Promise, ExecutionContext}
import java.nio.ByteBuffer
import java.io.File
import scala.util._
import concurrent.stm._


class AsyncFileOutputStream(file: File)(implicit val ec: ExecutionContext)
    extends AbstractAsyncOutputStream {

  def write(bytes: ByteBuffer): Future[Int] = {
    val writePosition = registerNewJob(bytes)
    asyncWritePromise(bytes, writePosition).future
  }

  def flush() = Future {
    try {
      instance.force(forceWithMetadata.single.getAndTransform(x => false))
    }
    catch {
      case ex: ClosedChannelException =>
        // does nothing
    }
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

  /**
   * @return The position in the file at which the write should happen
   */
  private[this] def registerNewJob(buffer: ByteBuffer): Long =
    signalJobStart { implicit txn =>
      lastPosition.getAndTransform(_ + buffer.limit() - buffer.position())
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

  private[this] val forceWithMetadata = Ref(initialValue = true)
}
