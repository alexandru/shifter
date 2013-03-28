package shifter.io

import concurrent.{ExecutionContext, Future}
import shifter.units._
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException


class AsyncBufferedOutputStream(out: AsyncOutputStream, bufferSize: Int = 8.megabytes)(implicit val ec: ExecutionContext)
    extends AsyncOutputStream {

  require(out != null, "output stream should not be null")
  require(bufferSize >= 1024, "output buffer should be at least 1 kilobyte")

  def write(bytes: ByteBuffer): Future[Int] = {
    val toWrite = synchronized {
      if (bytes.limit() > bufferSize)
        throw new IllegalArgumentException("Cannot write chunks bigger than the buffer size")
      if (isClosedRef)
        throw new ClosedChannelException

      if (leftCapacity(buffer) >= bytes.limit()) {
        buffer.put(bytes)
        null
      }
      else {
        buffer.flip()
        val toWrite = buffer.asReadOnlyBuffer()

        buffer = allocateNewBuffer
        buffer.put(bytes)

        toWrite
      }
    }

    if (toWrite != null)
      out.write(toWrite)
    else
      zeroBytesWritten
  }

  def flush(): Future[Unit] = {
    val toWrite = synchronized {
      if (buffer.position() > 0) {
        buffer.flip()
        val toWrite = buffer.asReadOnlyBuffer()

        buffer = allocateNewBuffer
        toWrite
      }
      else
        null
    }

    if (toWrite != null)
      out.write(toWrite).flatMap(x => out.flush())
    else
      out.flush()
  }

  def forceClose() {
    out.forceClose()
  }

  def isClosed: Boolean =
    synchronized(isClosedRef)

  def asyncClose(): Future[Unit] = {
    synchronized { isClosedRef = true }
    flush().flatMap(_ => out.asyncClose())
  }

  private[this] def leftCapacity(buffer: ByteBuffer) =
    buffer.limit() - buffer.position()

  private[this] def allocateNewBuffer =
    ByteBuffer.allocate(bufferSize)

  private[this] var isClosedRef = false
  private[this] var buffer = allocateNewBuffer
  private[this] val zeroBytesWritten = Future.successful(0)
}
