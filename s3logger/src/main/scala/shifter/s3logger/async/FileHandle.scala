package shifter.s3logger.async

import util.Try
import java.util.concurrent.locks.ReentrantReadWriteLock
import concurrent.stm._
import java.io.File
import shifter.io.{AsyncFileOutputStream, AsyncBufferedOutputStream, AsyncGZIPOutputStream, AsyncOutputStream}
import annotation.tailrec
import concurrent.duration._
import concurrent.ExecutionContext


final class FileHandle(prefix: String, suffix: String, localDir: Option[String])(implicit ec: ExecutionContext) {

  def write(content: String) {
    withHandler { out =>
      out.write(content.getBytes("UTF-8"))
    }
  }

  def rotate(destination: File): Boolean =
    state.single.get match {
      case Borrowed =>
        rotate(destination)
      case NotInitialized =>
        false
      case Available =>
        if (!state.single.compareAndSet(Available, Borrowed))
          rotate(destination)
        else {
          lock.writeLock().lock()
          try {
            // rotate file as fast as possible
            // first close the output stream
            val outStream = out.single.get
            outStream.close(10.seconds)

            val oldFile = file.single.get
            oldFile.renameTo(destination)

            atomic { implicit txn =>
              file() = null
              out() = null
              state() = NotInitialized
            }

            true
          }
          finally
            lock.writeLock().unlock()
        }
    }

  def hasWrites = state.single.get == Available

  def getFileSizeMB: Long = atomic { implicit txn =>
    // updates once every 3 seconds
    val lastUpdate = fileSizeOpTs() / 3000
    if (file() != null && lastUpdate > 0 && lastUpdate < System.currentTimeMillis() / 3000) {
      lastFileSize() = Try(file().length()).getOrElse(0L)
      fileSizeOpTs() = System.currentTimeMillis()
    }
    lastFileSize()
  }

  def startedAt = startedAtTs.single.get
  def lifespanInMillis = System.currentTimeMillis() - startedAtTs.single.get

  @tailrec
  private[this] def withHandler[T](cb: AsyncOutputStream => T): T =
    state.single.get match {
      case Borrowed =>
        withHandler(cb)
      case Available =>
        lock.readLock().lock()
        try {
          cb(out.single.get)
        }
        finally {
          lock.readLock().unlock()
        }
      case NotInitialized =>
        if (!state.single.compareAndSet(NotInitialized, Borrowed))
          withHandler(cb)
        else {
          lock.writeLock().lock()
          try {
            val newFile = localDir match {
              case Some(dir) => File.createTempFile(prefix, suffix, new File(dir))
              case None => File.createTempFile(prefix, suffix)
            }

            val newOut  = new AsyncGZIPOutputStream(
              new AsyncBufferedOutputStream(
                new AsyncFileOutputStream(newFile)))

            atomic { implicit txn =>
              file() = newFile
              out() = newOut
              state() = Available
              startedAtTs() = System.currentTimeMillis()
            }
          }
          finally
            lock.writeLock().unlock()

          withHandler(cb)
        }
    }
  private[this] val startedAtTs = Ref(0L)
  private[this] val lock = new ReentrantReadWriteLock()
  private[this] val file = Ref(null : File)
  private[this] val out = Ref(null : AsyncOutputStream)
  private[this] val state = Ref(NotInitialized : FileHandleState)

  private[this] val fileSizeOpTs = Ref(0L)
  private[this] val lastFileSize = Ref(0L)
}
