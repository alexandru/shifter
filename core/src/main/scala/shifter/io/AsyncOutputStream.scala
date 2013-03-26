package shifter.io

import java.io.Closeable
import concurrent.Future

trait AsyncOutputStream extends Closeable {
  def write(bytes: Array[Byte]): Future[Int]
  def flush()
  def close()
  def tryClose(): Future[Unit]
}
