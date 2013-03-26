package shifter.io

import shifter.concurrency.extensions._
import java.nio.channels.{CompletionHandler, AsynchronousFileChannel}
import collection.JavaConverters._
import java.nio.file.StandardOpenOption
import concurrent.{Future, Promise, ExecutionContext}
import java.nio.ByteBuffer
import java.io.File
import scala.util._
import concurrent.stm._
import annotation.tailrec


class AsyncFileOutputStream(file: File, append: Boolean = false, ordered: Boolean = false)(implicit ec: ExecutionContext)
    extends AsyncOutputStream {

  def write(bytes: Array[Byte]): Future[Int] = {
    val writePosition = if (ordered)
      synchronized(lastPosition.single.getAndTransform(_ + bytes.length))
    else
      lastPosition.single.getAndTransform(_ + bytes.length)

    pendingJobs.single.transform(_ + 1)
    asyncWritePromise(bytes, writePosition).future
  }

  def flush() {
    instance.force(forceWithMetadata)
    if (forceWithMetadata)
      forceWithMetadata = false
  }

  private[this] val writeCompletionHandler = new CompletionHandler[Integer, Promise[Int]] {
    def completed(result: Integer, promise: Promise[Int]) {
      promise.complete(Success(result))
      pendingJobs.single.transform(_ - 1)
    }
    def failed(exc: Throwable, promise: Promise[Int]) {
      promise.complete(Failure(exc))
      pendingJobs.single.transform(_ - 1)
    }
  }

  private[this] def asyncWritePromise[T](bytes: Array[Byte], positionInFile: Long): Promise[Int] = {
    val promise = Promise[Int]()
    val buffer = ByteBuffer.wrap(bytes)
    instance.write(buffer, positionInFile, promise, writeCompletionHandler)
    promise
  }

  def close() {
    instance.close()
  }

  def tryClose(): Future[Unit] =
    initiatedClose.single.get match {
      case Some(future) => future
      case None =>
        this.synchronized {
          val ref = initiatedClose.single.transformAndGet {
            case ref @ Some(_) => ref
            case None => Some(Future(waitPendingZero()))
          }
          ref.get
        }
    }

  @tailrec
  private[this] def waitPendingZero() {
    if (pendingJobs.single.get != 0) {
      Thread.sleep(20)
      waitPendingZero()
    }
  }

  private[this] val initiatedClose = Ref(None : Option[Future[Unit]])

  private[this] val lastPosition = {
    if (file.exists() && append)
      Ref(file.length())
    else
      Ref(0L)
  }

  private[this] val options =
    if (file.exists() && append)
      Seq(StandardOpenOption.CREATE, StandardOpenOption.WRITE).toSet.asJava
    else
      Seq(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).toSet.asJava

  private[this] val pendingJobs = Ref(0L)
  private[this] val instance =
    AsynchronousFileChannel.open(file.toPath, options, ec.toExecutorService)

  @volatile
  private[this] var forceWithMetadata = true
}
