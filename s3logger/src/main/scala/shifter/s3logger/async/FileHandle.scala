package shifter.s3logger.async

import util.Try
import java.io.{IOException, File}
import shifter.io.{AsyncFileOutputStream, AsyncBufferedOutputStream, AsyncGZIPOutputStream, AsyncOutputStream}
import annotation.tailrec
import concurrent.duration._
import concurrent.ExecutionContext
import java.nio.ByteBuffer
import shifter.concurrency.atomic.Ref


final class FileHandle(prefix: String, suffix: String, localDir: Option[String])(implicit ec: ExecutionContext) {
  def write(content: String, charset: String = "UTF-8") {
    withHandler { out =>
      out.write(content.getBytes(charset))
    }
  }

  def write(b: Array[Byte]) {
    withHandler { out =>
      out.write(b)
    }
  }

  def write(b: Array[Byte], off: Int, len: Int) {
    withHandler { out =>
      out.write(b, off, len)
    }
  }

  def write(b: ByteBuffer) {
    withHandler { out =>
      out.write(b)
    }
  }

  def rotate(destination: File): Boolean =
    stateRef.get match {
      case NotInitialized =>
        false

      case state @ Available(startedAtTs, file, out) =>
        if (!stateRef.compareAndSet(state, NotInitialized))
          rotate(destination)

        else
          rotateLock.synchronized {
            // rotate file as fast as possible
            // first close the output stream
            out.close(1.minute)
            file.renameTo(destination)
            true
          }
    }

  def hasWrites =
    stateRef.get != NotInitialized

  def getFileSizeMB: Long = {
    val lastUpdate = fileSizeOpTs.get / 3000
    if (lastUpdate == 0 || lastUpdate < System.currentTimeMillis() / 3000)
      lastFileSize.transformAndGet { current =>
        stateRef.get match {
          case Available(_, file, _) =>
            Try(file.length()).getOrElse(0L)
          case _ =>
            current
        }
      }
    else
      lastFileSize.get
  }

  def startedAt = stateRef.get match {
    case Available(ts, _, _) => ts
    case NotInitialized => 0L
  }

  def lifespanInMillis = stateRef.get match {
    case Available(ts, _, _) => System.currentTimeMillis() - ts
    case NotInitialized => 0L
  }

  @tailrec
  private[this] def withHandler[T](cb: AsyncOutputStream => T): T =
    stateRef.get match {
      case current @ Available(_, _, out) =>
        val result = try {
          cb(out)
        }
        catch {
          case ex: Throwable => ex
        }

        if (result.isInstanceOf[Throwable])
          if (result.isInstanceOf[IOException])
            if (stateRef.compareAndSet(current, current))
              throw result.asInstanceOf[IOException]
            else
              withHandler(cb)
          else
            throw result.asInstanceOf[Throwable]
        else
          result.asInstanceOf[T]

      case NotInitialized =>
        initializeLock.synchronized {
          if (stateRef.compareAndSet(NotInitialized, NotInitialized)) {
            val newFile = localDir match {
              case Some(dir) => File.createTempFile(prefix, suffix, new File(dir))
              case None => File.createTempFile(prefix, suffix)
            }

            newFile.deleteOnExit()

            val newOut  = new AsyncGZIPOutputStream(
              new AsyncBufferedOutputStream(
                new AsyncFileOutputStream(newFile)))

            val stateTest = stateRef.compareAndSet(NotInitialized, Available(
              startedAtTs = System.currentTimeMillis(),
              file = newFile,
              out = newOut
            ))

            if (!stateTest)
              throw new IllegalStateException()
          }
        }

        withHandler(cb)
    }

  private[this] val initializeLock = new AnyRef
  private[this] val rotateLock = new AnyRef
  private[this] val stateRef = Ref(NotInitialized : FileHandleState)
  private[this] val fileSizeOpTs = Ref(0L)
  private[this] val lastFileSize = Ref(0L)
}
