package shifter.io

import concurrent.{Promise, Future, ExecutionContext}
import java.nio.ByteBuffer
import shifter.units._
import java.util.zip.{Deflater, CRC32}
import java.io.IOException
import java.nio.channels.ClosedChannelException


class AsyncGZIPOutputStream(out: AsyncOutputStream, bufferSize: Int = 1.megabyte, compressionLevel: Int = -1, syncFlush: Boolean = false)(implicit val ec: ExecutionContext)
    extends AsyncOutputStream {

  require(out != null, "output stream should not be null")
  require(bufferSize >= 1024, "output buffer should be at least 1 kilobyte")
  require(compressionLevel == -1 || (compressionLevel >= 0 && compressionLevel <= 9),
    "compressionLevel must be a value between 0 and 9, or -1")

  override def write(b: Array[Byte], off: Int, len: Int): Future[Int] = {
    val toWrite = synchronized {
      if ((off | len | (off + len) | (b.length - (off + len))) < 0)
        throw new IndexOutOfBoundsException
      if (isClosedRef)
        throw new ClosedChannelException
      if (deflater.finished())
        throw new IOException("write beyond end of stream")

      var arrayBuffer: ByteBuffer = null

      if (!headerWritten) {
        arrayBuffer = putIntoBuffer(arrayBuffer, GZIP_HEADER, 0, GZIP_HEADER.length)
        headerWritten = true
      }

      crc.update(b, off, len)
      deflater.setInput(b, off, len)

      while (!deflater.needsInput()) {
        val flush = if (syncFlush) Deflater.SYNC_FLUSH else Deflater.NO_FLUSH
        val compressedLen = deflater.deflate(buffer, 0, buffer.length, flush)
        if (compressedLen > 0)
          arrayBuffer = putIntoBuffer(arrayBuffer, buffer, 0, compressedLen)
      }

      if (arrayBuffer != null && arrayBuffer.position() > 0) {
        arrayBuffer.flip()
        arrayBuffer.asReadOnlyBuffer()
      }
      else
        null
    }

    if (toWrite != null)
      out.write(toWrite)
    else
      Future.successful(0)
  }

  def write(bytes: ByteBuffer): Future[Int] =
    if (bytes.hasArray) {
      val offset = bytes.arrayOffset() + bytes.position()
      val length = bytes.limit() - bytes.position()
      write(bytes.array(), offset, length)
    }
    else if (bytes.limit() > bytes.position()) {
      // copy array
      val length = bytes.limit() - bytes.position()
      val dst = Array.fill(length)(ZERO)
      bytes.get(dst, 0, length)
      write(dst, 0, length)
    }
    else
      write(emptyArray, 0, 0)

  def isClosed: Boolean =
    synchronized(isClosedRef)

  def asyncClose(): Future[Unit] = {
    val (toWrite, isDeflaterFinished, isAlreadyClosed) =
      synchronized {
        if (isClosedRef != true) {
          isClosedRef = true

          // fetching buffer to flush, if anything is left
          if (!deflater.finished())
            flushedBuffer(finish = true) match {
              case Some(byteBuffer) if byteBuffer.limit() > 0 =>
                (byteBuffer, false, false)
              case _ =>
                (null, false, false)
            }
          else
            (null, true, false)
        }
        else
          (null, true, true)
      }

    if (!isAlreadyClosed)
      if (!isDeflaterFinished)
        if (toWrite != null)
          closePromise.tryCompleteWith(
            // writing buffer left
            out.write(toWrite).flatMap(_ =>
              // then do a flush (just in case)
              out.flush()).flatMap(_ =>
              // then signal close
              out.asyncClose()))
        else
          closePromise.tryCompleteWith(
            // nothing left to write, so first do a flush
            out.flush().flatMap(_ =>
              // then close
              out.asyncClose()))

      else
        // deflater is already finished, just close
        closePromise.tryCompleteWith(out.asyncClose())

    closePromise.future
  }

  def flush(): Future[Unit] = {
    val toWrite = synchronized {
      if (!deflater.finished())
        flushedBuffer(finish = false) match {
          case Some(byteBuffer) if byteBuffer.limit() > 0 =>
            byteBuffer
          case _ =>
            null
        }
      else
        null
    }

    if (toWrite != null)
      out.write(toWrite).flatMap(r => out.flush())
    else
      out.flush()
  }

  def forceClose() {
    out.forceClose()
  }

  private[this] def flushedBuffer(finish: Boolean): Option[ByteBuffer] = synchronized {
    if (finish)
      deflater.finish()

    var byteBuffer: ByteBuffer = null
    var bytesWritten = 0

    if (!headerWritten) {
      writeTrailer(buffer, 0)
      byteBuffer = putIntoBuffer(byteBuffer, GZIP_HEADER, 0, GZIP_HEADER.length)
      bytesWritten += GZIP_HEADER.length
      headerWritten = true
    }

    var compressedLength = 0
    var shouldContinue = finish

    do {
      compressedLength = deflater.deflate(buffer, 0, buffer.length, Deflater.FULL_FLUSH)
      shouldContinue = finish && !deflater.finished()

      if (compressedLength > 0)
        byteBuffer = putIntoBuffer(byteBuffer, buffer, 0, compressedLength)
    } while(compressedLength > 0 || shouldContinue)

    if (finish) {
      writeTrailer(buffer, 0)
      byteBuffer = putIntoBuffer(byteBuffer, buffer, 0, TRAILER_SIZE)
      bytesWritten += TRAILER_SIZE
      deflater.reset()
    }

    if (byteBuffer != null) {
      byteBuffer.flip()
      Some(byteBuffer.asReadOnlyBuffer())
    }
    else
      None
  }

  private[this] def putIntoBuffer(buffer: ByteBuffer, b: Array[Byte], offset: Int, length: Int): ByteBuffer = {
    val currentBuffer = if (buffer != null)
      buffer
    else
      allocateNewByteBuffer(math.max(bufferSize, length))

    val remainingLength = currentBuffer.limit() - currentBuffer.position()
    val writeBuffer = if (remainingLength < length)
      expandArrayBuffer(currentBuffer, currentBuffer.capacity() + length * 2)
    else
      currentBuffer

    writeBuffer.put(b, offset, length)
    writeBuffer
  }

  private[this] def expandArrayBuffer(buffer: ByteBuffer, newCapacity: Int) = {
    val newBuffer = allocateNewByteBuffer(newCapacity)
    buffer.flip()
    newBuffer.put(buffer)
    newBuffer
  }

  private[this] def writeShort(i: Int, buf: Array[Byte], offset: Int) {
    buf(offset) = (i & 0xff).asInstanceOf[Byte]
    buf(offset + 1) = ((i >> 8) & 0xff).asInstanceOf[Byte]
  }

  private[this] def writeInt(i: Int, buf: Array[Byte], offset: Int) {
    writeShort(i & 0xffff, buf, offset)
    writeShort((i >> 16) & 0xffff, buf, offset + 2)
  }

  private[this] def writeTrailer(buf: Array[Byte], offset: Int) {
    writeInt(crc.getValue.asInstanceOf[Int], buf, offset)
    writeInt(deflater.getTotalIn, buf, offset + 4)
  }

  private[this] def allocateNewByteBuffer(length: Int) =
    ByteBuffer.allocate(length)

  private[this] val TRAILER_SIZE = 8
  private[this] val ZERO = 0.asInstanceOf[Byte]
  private[this] val GZIP_MAGIC = 0x8b1f
  private[this] val GZIP_HEADER = Array(
    GZIP_MAGIC.asInstanceOf[Byte],
    (GZIP_MAGIC >> 8).asInstanceOf[Byte],
    Deflater.DEFLATED.asInstanceOf[Byte],
    ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO
  )

  private[this] val crc = {
    val obj = new CRC32
    obj.reset()
    obj
  }

  private[this] val deflater = {
    val obj = new Deflater(Deflater.DEFAULT_COMPRESSION, true)
    obj.setLevel(compressionLevel)
    obj
  }

  private[this] var isClosedRef = false
  private[this] var headerWritten = false
  private[this] val buffer = Array.fill(bufferSize + 1024)(ZERO)
  private[this] val emptyArray = Array.empty[Byte]
  private[this] val closePromise = Promise[Unit]()
}
