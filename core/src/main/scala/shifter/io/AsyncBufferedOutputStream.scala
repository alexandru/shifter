package shifter.io

import concurrent.{ExecutionContext, Promise, Future}
import collection.mutable.ArrayBuffer
import shifter.units._


class AsyncBufferedOutputStream(out: AsyncOutputStream, bufferSize: Int = 8.megabytes)(implicit ec: ExecutionContext)
    extends AsyncOutputStream {

  private[this] val buffer = {
    val buf = ArrayBuffer[Byte]()
    buf.sizeHint(bufferSize * 2)
    buf
  }

  private[this] var pendingPromise: Promise[Int] = null
  private[this] var pendingFuture: Future[Int] = null


  def flush() {
    // TODO
    out.flush()
  }

  def write(bytes: Array[Byte]): Future[Int] =
    this.synchronized {
      buffer.append(bytes :_*)
      sendBuffer(forced = false)
    }

  def tryClose(): Future[Unit] =
    sendBuffer(forced = true).recover { case _ => 0 }.flatMap {
      case _ =>
        out.tryClose()
    }

  def close() {
    out.close()
  }

  private[this] def sendBuffer(forced: Boolean): Future[Int] =
    this.synchronized {
      if (pendingPromise == null) {
        pendingPromise = Promise[Int]()
        pendingFuture = pendingPromise.future
      }

      val newSize = buffer.size

      if (newSize >= bufferSize || (forced && newSize > 0)) {
        val promise = pendingPromise
        val future = pendingFuture

        out.write(buffer.toArray).onComplete {
          case result =>
            out.flush()
            promise.complete(result)
        }

        buffer.clear()
        pendingPromise = null
        pendingFuture = null
        future
      }
      else if (forced && newSize == 0)
        Future.successful(0)
      else
        pendingFuture
    }
}
