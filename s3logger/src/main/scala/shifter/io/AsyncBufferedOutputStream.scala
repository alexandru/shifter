package shifter.io

import concurrent.{Promise, ExecutionContext, Future}
import shifter.units._
import concurrent.stm._
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException


class AsyncBufferedOutputStream(out: AsyncOutputStream, bufferSize: Int = 8.megabytes)(implicit val ec: ExecutionContext)
    extends AsyncOutputStream {

  require(out != null, "output stream should not be null")
  require(bufferSize >= 1024, "output buffer should be at least 1 kilobyte")

  def write(bytes: ByteBuffer): Future[Int] = synchronized {
    if (bytes.limit() > bufferSize)
      throw new IllegalArgumentException("Cannot write chunks bigger than the buffer size")
    if (isClosedRef)
      throw new ClosedChannelException

    if (leftCapacity(buffer) >= bytes.limit()) {
      buffer.put(bytes)
      writesPending.single.transform(_ + 1)
      zeroBytesWritten
    }
    else {
      buffer.flip()
      val toWrite = buffer
      val future = out.write(toWrite)

      buffer = allocateNewBuffer
      buffer.put(bytes)
      writesPending.single.set(1)

      future
    }
  }

  private[this] def flushBuffer(buffer: ByteBuffer, promise: Promise[Int], writesNr: Int): Future[Int] =
    out.write(buffer).andThen {
      case r =>
        promise.complete(r)
    }

  def flush(): Future[Unit] = synchronized {
    if (buffer.position() > 0) {
      buffer.flip()

      val toWrite = buffer
      val writeFuture = out.write(toWrite)

      buffer = allocateNewBuffer
      writesPending.single.set(0)

      writeFuture.flatMap(x => out.flush())
    }
    else
      out.flush()
  }

  def forceClose() {
    out.forceClose()
  }

  def isClosed: Boolean = synchronized(isClosedRef)

  def asyncClose(): Future[Unit] = synchronized {
    isClosedRef = true
    flush().flatMap(_ => out.asyncClose())
  }

  private[this] def leftCapacity(buffer: ByteBuffer) =
    buffer.limit() - buffer.position()

  private[this] val directAllocation =
    out.isInstanceOf[AsyncFileOutputStream]

  private[this] def allocateNewBuffer =
    if (directAllocation)
      try
        ByteBuffer.allocateDirect(bufferSize)
      catch {
        case ex: OutOfMemoryError if ex.getMessage.trim.toLowerCase == "direct buffer memory" =>
          ByteBuffer.allocate(bufferSize)
      }
    else
      ByteBuffer.allocate(bufferSize)

  private[this] var isClosedRef = false
  private[this] var buffer = allocateNewBuffer
  private[this] val writesPending = Ref(0)
  private[this] val zeroBytesWritten = Future.successful(0)
}
