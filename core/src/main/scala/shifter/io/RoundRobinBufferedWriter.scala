package shifter.io

import java.io.Writer
import shifter.units._
import scala.concurrent.atomic.Atomic


/**
 * A [[http://docs.oracle.com/javase/1.5.0/docs/api/java/io/Writer.html Writer]]
 * that buffers content to write in multiple char buffers, to avoid synchronization issues
 * and blocking.
 *
 * When a write comes in, a char buffer is picked using a round-robin selection process.
 * The idea is that when multiple threads are writing to an instance of `RoundRobinBufferedWriter`,
 * each of them usually end up writing to different locations in memory.
 *
 * It does not provide any guarantees about the order of writes. Useful for log files, where
 * the order of the lines written don't matter.
 *
 * @param out - the writer for piping the buffered content
 * @param capacity - the size of a single in-memory buffer (the total size will be `capacity` * number of buffers used)
 * @param parallelismFactor - used to calculate the number of buffers to instantiate, based on the number of processors available
 */
final class RoundRobinBufferedWriter(out: Writer, capacity: Int = 1.megabyte, parallelismFactor: Int = 1) extends Writer {
  def write(charsToWrite: Array[Char], off: Int, len: Int) {
    if (len > capacity)
      throw new IllegalArgumentException("RoundRobinBufferedWriter cannot write lines bigger than %d".format(capacity))
    else if (charsToWrite == null)
      throw new NullPointerException()
    else if (off + len > charsToWrite.length)
      throw new ArrayIndexOutOfBoundsException()

    else if (len > 0) {
      withBuffer { buffer =>
        if (buffer.position.get + len > capacity)
          flushBuffer(buffer)

        val position = buffer.position.getAndTransform(_ + len)
        System.arraycopy(charsToWrite, off, buffer.array, position, len)
      }
    }
  }

  private[this] def flushBuffer(buffer: CharBuffer) {
    buffer.synchronized {
      val position = buffer.position.getAndSet(0)
      if (position > 0)
        out.write(buffer.array, 0, position)
    }
  }

  def flush() {
    out.synchronized {
      for (buffer <- allBuffers)
        flushBuffer(buffer)
      out.flush()
    }
  }

  def close() {
    out.synchronized {
      flush()
      out.close()
    }
  }

  private[this] final case class CharBuffer(array: Array[Char], position: Atomic[Int])

  private[this] def withBuffer[T](cb: CharBuffer => T): T = {
    val buffer = allBuffers(roundRobinCnt.getAndIncrement % allBuffers.length)

    buffer.synchronized {
      cb(buffer)
    }
  }

  private[this] def initBuffer =
    CharBuffer(
      array = new Array[Char](capacity),
      position = Atomic(0)
    )

  private[this] val roundRobinCnt = Atomic(0)

  private[this] val number = {
    math.max(Runtime.getRuntime.availableProcessors(), 1) * parallelismFactor
  }

  private[this] val allBuffers: Vector[CharBuffer] = (0 until number)
    .map { _ => initBuffer }.toVector
}

