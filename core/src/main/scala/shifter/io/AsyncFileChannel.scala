package shifter.io

import java.io.File
import scala.concurrent.{ExecutionContext, Future, Promise}
import java.nio.file.StandardOpenOption
import java.nio.channels.{CompletionHandler, AsynchronousFileChannel}
import scala.util.{Failure, Success}
import java.nio.ByteBuffer
import collection.JavaConverters._
import shifter.io.Implicits.IOContext
import shifter.concurrency.extensions._


/**
 * Wrapper around
 * [[http://openjdk.java.net/projects/nio/javadoc/java/nio/channels/AsynchronousFileChannel.html java.nio.channels.AsynchronousFileChannel]]
 * (class available since Java 7 for doing async I/O on files).
 *
 * @param file - to open
 * @param options - specify the mode for opening the file
 *
 * @example {{{
 *
 *   val out = new AsyncFileChannel(File.createTempFile, StandardOpenOption.CREATE)
 *
 *   val bytes = ByteBuffer.wrap("Hello world!".getBytes("UTF-8"))
 *   val future = out.write(bytes, 0)
 *
 *   future.onComplete {
 *     case Success(nr) =>
 *       println("Bytes written: %d".format(nr))
 *
 *     case Failure(exc) =>
 *       println(s"ERROR: " + exc.getMessage)
 *   }
 *
 * }}}
 */
final class AsyncFileChannel(file: File, options: StandardOpenOption*) {
  private[this] val ec = implicitly[ExecutionContext]

  /**
   * Writes a sequence of bytes to this channel from the given buffer,
   * starting at the given file position.
   *
   * If the given position is greater than the file's size, at the time that the write is
   * attempted, then the file will be grown to accommodate the new bytes; the values of any
   * bytes between the previous end-of-file and the newly-written bytes are unspecified.
   *
   * @param source - the sequence of bytes to write
   * @param positionInFile - the position in file where to write.
   * @return - a future value containing the number of bytes written
   */
  def write(source: ByteBuffer, positionInFile: Long): Future[Int] = {
    val promise = Promise[Int]()
    instance.write(source, positionInFile, promise, writeCompletionHandler)
    promise.future
  }

  /**
   * Reads a sequence of bytes from this channel into the given buffer,
   * starting at the given file position.
   *
   * @param dest - the buffer holding the bytes read on completion
   * @param positionInFile - the position in file from where to read
   * @return - the number of bytes read or -1 if the given position is
   *         greater than or equal to the file's size at the time the read
   *         is attempted
   */
  def read(dest: ByteBuffer, positionInFile: Long): Future[Int] = {
    val promise = Promise[Int]()
    instance.read(dest, positionInFile, promise, readCompletionHandler)
    promise.future
  }

  /**
   * Returns the current size of this channel's file.
   */
  def size = instance.size()

  /**
   * Forces any updates to this channel's file to be written to the storage device that contains it.
   *
   * @param metadata - can be used to limit the number of I/O operations that this method is
   *                 required to perform. Passing false for this parameter indicates that only
   *                 updates to the file's content need be written to storage; passing true
   *                 indicates that updates to both the file's content and metadata must be written,
   *                 which generally requires at least one more I/O operation. Whether this parameter
   *                 actually has any effect is dependent upon the underlying operating system and
   *                 is therefore unspecified.
   */
  def force(metadata: Boolean) {
    instance.force(metadata)
  }

  /**
   * Closes this channel.
   *
   * Any outstanding asynchronous operations upon this channel will complete with the exception
   * `AsynchronousCloseException`.
   */
  def close() {
    instance.close()
  }

  private[this] val readCompletionHandler = new CompletionHandler[Integer, Promise[Int]] {
    def completed(result: Integer, promise: Promise[Int]) {
      promise.complete(Success(result))
    }

    def failed(exc: Throwable, promise: Promise[Int]) {
      promise.complete(Failure(exc))
    }
  }

  private[this] val writeCompletionHandler = new CompletionHandler[Integer, Promise[Int]] {
    def completed(result: Integer, promise: Promise[Int]) {
      promise.complete(Success(result))
    }

    def failed(exc: Throwable, promise: Promise[Int]) {
      try {
        promise.complete(Failure(exc))
      }
      finally {
        ec.reportFailure(exc)
      }
    }
  }

  private[this] val instance =
    AsynchronousFileChannel.open(file.toPath, options.toSet.asJava, ec.toExecutorService)
}
