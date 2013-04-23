package shifter.io

import java.io.OutputStream
import java.nio.ByteBuffer
import scala.concurrent.Future

/**
 * An `OutputStream` with added async variants for non-blocking I/O.
 */
trait AsyncOutputStream extends OutputStream {
  /**
   * Writes to this `OutputStream`, returning the number of
   * bytes written on completion.
   *
   * The byte buffer passed that holds the bytes to write
   * should be immutable.
   */
  def asyncWrite(b: ByteBuffer): Future[Int]

  /**
   * Closes this `OutputStream` after all outstanding
   * `asyncWrite` operations are completed.
   *
   * Returns a `Future` that completes after the close
   * was actually accomplished.
   */
  def asyncClose(): Future[Unit]

  /**
   * Flushes content to disk, after all outstanding
   * `asyncWrite` operations are completed, triggering
   * a forced I/O operation.
   *
   * Returns a `Future` that completes after the flush
   * operation was actually accomplished.
   */
  def asyncFlush(): Future[Unit]
}
