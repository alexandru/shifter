package shifter.io

import java.io.Closeable
import concurrent.{Await, Future}
import concurrent.duration._
import java.nio.ByteBuffer
import java.util.concurrent.TimeoutException

trait AsyncOutputStream extends Closeable {
  def write(buffer: ByteBuffer): Future[Int]

  def isClosed: Boolean

  def asyncClose(): Future[Unit]

  def flush(): Future[Unit]

  def forceClose()

  def write(b: Array[Byte]): Future[Int] =
    write(ByteBuffer.wrap(b))

  def write(b: Array[Byte], off: Int, len: Int): Future[Int] =
    if ((off | len | (off + len) | (b.length - (off + len))) < 0)
      throw new IndexOutOfBoundsException
    else
      write(ByteBuffer.wrap(b, off, len))

  def close() {
    close(5.seconds)
  }

  def close(waitAtMost: FiniteDuration) {
    try {
      Await.result(asyncClose(), waitAtMost)
      forceClose()
    }
    catch {
      case ex: TimeoutException =>
        forceClose()
        throw ex
    }
  }
}
