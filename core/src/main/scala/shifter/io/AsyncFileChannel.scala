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


final class AsyncFileChannel(file: File, options: StandardOpenOption*) {
  private[this] val ec = implicitly[ExecutionContext]

  def write(source: ByteBuffer, positionInFile: Long): Future[Int] = {
    val promise = Promise[Int]()
    instance.write(source, positionInFile, promise, writeCompletionHandler)
    promise.future
  }

  def read(dest: ByteBuffer, positionInFile: Long): Future[Int] = {
    val promise = Promise[Int]()
    instance.read(dest, positionInFile, promise, readCompletionHandler)
    promise.future
  }

  def size = instance.size()

  def force(metadata: Boolean) {
    instance.force(metadata)
  }

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
