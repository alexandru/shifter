package shifter.io

import java.io.OutputStream
import java.nio.ByteBuffer
import scala.concurrent.Future

trait AsyncOutputStream extends OutputStream {
  def asyncWrite(b: ByteBuffer): Future[Int]
  def asyncClose(): Future[Unit]
  def asyncFlush(): Future[Unit]
}
