package shifter.io

import scala.concurrent.{Promise, Future}
import java.io.File
import java.nio.file.{StandardOpenOption => o}
import java.nio.ByteBuffer
import scala.util.Try
import scala.concurrent.duration.Duration
import shifter.concurrency.extensions._
import shifter.io.Implicits.IOContext
import scala.concurrent.atomic.Atomic


/**
 * An asynchronous file output stream is an output stream for
 * writing data to a `File` in an asynchronous manner.
 *
 * The write operations are guaranteed to be atomic and non-blocking.
 */
final class AsyncFileOutputStream(file: File) extends AsyncOutputStream {
  def asyncWrite(b: ByteBuffer): Future[Int] = {
    val length = b.limit() - b.position()
    val buffer: ByteBuffer = if (b.isReadOnly)
      b
    else {
      val buffer = ByteBuffer.allocate(length)
      buffer.put(b)
      buffer.flip()
      buffer.asReadOnlyBuffer()
    }

    val position = positionRef.getAndTransform(_ + length)
    val channelFuture = instance.write(buffer, position)

    registerNewFuture { promise =>
      promise.completeWith(channelFuture)
    }
  }

  def write(b: Int) {
    val buf = ByteBuffer.allocate(1)
    buf.put(b.toByte).flip()
    asyncWrite(buf.asReadOnlyBuffer())
  }

  override def write(b: Array[Byte]) {
    asyncWrite(wrapArray(b, 0, b.length))
  }

  override def write(b: Array[Byte], off: Int, len: Int) {
    asyncWrite(wrapArray(b, off, len))
  }

  def asyncFlush() =
    registerNewFuture { promise =>
      promise.complete(Try {
        instance.force(metadata = true)
        0
      })
    }
    .map(_ => ())

  def asyncClose() =
    registerNewFuture { promise =>
      promise.complete(Try {
        instance.force(metadata = true)
        instance.close()
        0
      })
    }
    .map(_ => ())


  override def flush() {
    asyncFlush().await(Duration.Inf)
  }

  override def close() {
    asyncClose().await(Duration.Inf)
  }

  private[this] def wrapArray(b: Array[Byte], off: Int, len: Int) = {
    val copy = new Array[Byte](len)
    System.arraycopy(b, off, copy, 0, len)
    ByteBuffer.wrap(copy).asReadOnlyBuffer()
  }

  private[this] def registerNewFuture(cb: Promise[Int] => Any): Future[Int] = {
    val newPromise = Promise[Int]()
    val newFuture = newPromise.future
    val lastFuture = lastFutureRef.getAndTransform(_ => newFuture)

    lastFuture.onComplete { case _ => cb(newPromise) }
    newFuture
  }

  private[this] val lastFutureRef = Atomic(Future.successful(0))
  private[this] val positionRef = Atomic(0L)
  private[this] val instance = {
    if (file.exists()) file.delete()
    new AsyncFileChannel(file, o.CREATE_NEW, o.WRITE)
  }
}
