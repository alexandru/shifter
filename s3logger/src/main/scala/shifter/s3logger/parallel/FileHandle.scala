package shifter.s3logger.parallel

import java.util.concurrent.atomic.AtomicReference
import java.io._
import annotation.tailrec
import java.util.zip.GZIPOutputStream
import util.Try

final class FileHandle(prefix: String, suffix: String, localDir: Option[String]) {
  def write(content: String) {
    withWriter {
      out =>
        out.write(content)
    }
  }

  @tailrec
  def rotate(destination: File): Boolean =
    state.get() match {
      case NotInitialized =>
        false
      case Borrowed =>
        rotate(destination)
      case Available =>
        if (!state.compareAndSet(Available, Borrowed))
          rotate(destination)
        else
          lock.synchronized {
            Try(writer.close())
            Try(fileOutStream.close())
            file.renameTo(destination)

            file = null
            fileOutStream = null
            writer = null

            if (!state.compareAndSet(Borrowed, NotInitialized))
              throw new IllegalStateException("FileHandle should be in Borrowed state")
            true
          }
    }

  def hasWrites = state.get() != NotInitialized

  def getFileSizeMB: Long =
    if (hasWrites && fileOutStream != null)
      Try(fileOutStream.getChannel.size() / (1024L * 1024)).getOrElse(0L)
    else
      0L

  def startedAt = startedAtTs
  def lifespanInMillis = System.currentTimeMillis() - startedAtTs

  @tailrec
  private[this] def withWriter[T](cb: Writer => T): T =
    state.get() match {
      case Available =>
        if (!state.compareAndSet(Available, Borrowed))
          withWriter(cb)
        else
          try {
            assert(writer != null, "Writer reference should not be null")
            cb(writer)
          }
          finally {
            if (!state.compareAndSet(Borrowed, Available))
              throw new IllegalStateException("FileHandle should be in Borrowed state")
          }

      case Borrowed =>
        withWriter(cb)

      case NotInitialized =>
        lock.synchronized {
          // can have race condition here
          if (state.get() == NotInitialized) {
            file = if (localDir.isDefined)
              File.createTempFile(prefix, suffix, new File(localDir.get))
            else
              File.createTempFile(prefix, suffix)

            file.deleteOnExit()
            fileOutStream = new FileOutputStream(file)
            writer = new BufferedWriter(
              new OutputStreamWriter(
                new GZIPOutputStream(fileOutStream), "UTF-8"))
            startedAtTs = System.currentTimeMillis()
            if (!state.compareAndSet(NotInitialized, Available))
              throw new IllegalStateException("FileHandle should be in NotInitialized state")
          }
        }

        // retry
        withWriter(cb)
    }

  @volatile private[this] var writer: Writer = null
  @volatile private[this] var fileOutStream: FileOutputStream = null
  @volatile private[this] var file: File = null
  @volatile private[this] var startedAtTs: Long = 0

  private[this] val lock = new AnyRef
  private[this] val state =
    new AtomicReference(NotInitialized : FileHandleState)
}
