package shifter.io

import java.io.File
import scala.concurrent.{Future, Promise, ExecutionContext}
import java.nio.file.StandardOpenOption
import java.nio.channels.{CompletionHandler, AsynchronousFileChannel}
import shifter.concurrency.extensions._
import collection.JavaConverters._
import scala.util.{Failure, Success}
import java.nio.ByteBuffer

final class AsyncFileChannel(file: File, options: StandardOpenOption*)(implicit val ec: ExecutionContext) {
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
      promise.complete(Failure(exc))
    }
  }

  private[this] val instance =
    AsynchronousFileChannel.open(file.toPath, options.toSet.asJava, ec.toExecutorService)
}
