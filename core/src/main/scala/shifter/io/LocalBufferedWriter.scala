package shifter.io

import java.io.Writer
import shifter.units._
import shifter.concurrency.utils.Ref
import scala.collection.immutable.Queue


final class LocalBufferedWriter(out: Writer, capacity: Int = 1.megabyte) extends Writer {
  def write(charsToWrite: Array[Char], off: Int, len: Int) {
    if (len > capacity)
      throw new IllegalArgumentException("LocalBufferedWriter cannot write lines bigger than %d".format(capacity))
    else if (charsToWrite == null)
      throw new NullPointerException()
    else if (off + len > charsToWrite.length)
      throw new ArrayIndexOutOfBoundsException()

    else if (len > 0) {
      val buffer = bufferRef.get()
      if (buffer.position.get + len > capacity)
        flushBuffer(buffer)

      buffer.synchronized {
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
      for (buffer <- allBuffers.get)
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

  private[this] val allBuffers = Ref(Queue.empty[CharBuffer])

  private[this] val bufferRef = new ThreadLocal[CharBuffer] {
    override def initialValue(): CharBuffer = {
      val newBuffer = CharBuffer(
        array = Array.fill(capacity)(0.toChar),
        position = Ref(0)
      )
      allBuffers.transform(_.enqueue(newBuffer))
      newBuffer
    }
  }
}