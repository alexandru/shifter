package shifter.io

import java.io.Writer
import shifter.units._
import shifter.concurrency.atomic.Ref


final class RoundRobinBufferedWriter(out: Writer, capacity: Int = 1.megabyte) extends Writer {
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

  private[this] final case class CharBuffer(array: Array[Char], position: Ref[Int])

  private[this] def withBuffer[T](cb: CharBuffer => T): T = {
    val buffer = allBuffers(roundRobinCnt.getAndIncrement % allBuffers.length)

    buffer.synchronized {
      cb(buffer)
    }
  }

  private[this] def initBuffer =
    CharBuffer(
      array = Array.fill(capacity)(0.toChar),
      position = Ref(0)
    )

  private[this] val roundRobinCnt = Ref(0)

  private[this] val number = math.max(Runtime.getRuntime.availableProcessors(), 4)
  private[this] val allBuffers: Vector[CharBuffer] = (0 until number)
    .map { _ => initBuffer }.toVector
}

