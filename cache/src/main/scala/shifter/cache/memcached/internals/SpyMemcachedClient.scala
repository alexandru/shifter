package shifter.cache.memcached.internals

import net.spy.memcached._
import java.{util => jutil}
import java.net.InetSocketAddress
import concurrent.{Promise, Future}
import net.spy.memcached.transcoders.Transcoder
import net.spy.memcached.ops._
import scala.util.{Try, Failure, Success}
import concurrent.duration._
import shifter.cache.UnhandledStatusException
import scala.Some
import shifter.concurrency.extensions._

class SpyMemcachedClient(conn: ConnectionFactory, addresses: jutil.List[InetSocketAddress])
    extends MemcachedClient(conn, addresses) {

  private[this] implicit val ec = shifter.io.Implicits.IOContext

  def realAsyncGet[T](key: String, timeout: FiniteDuration): Future[Result[Option[T]]] = {
    val promise = Promise[Result[Option[T]]]()
    val result = new MutablePartialResult[Option[T]]

    val op: GetOperation = opFact.get(key, new GetOperation.Callback {
      def receivedStatus(opStatus: OperationStatus) {
        if (statusTranslation.isDefinedAt(opStatus))
          statusTranslation(opStatus) match {
            case CASNotFoundStatus =>
              result.tryComplete(Success(SuccessfulResult(key, None)))
            case CASSuccessStatus =>
              // nothing
            case failure =>
              result.tryComplete(Success(FailedResult(key, failure)))
          }

        else
          throw new UnhandledStatusException(
            "%s(%s)".format(opStatus.getClass, opStatus.getMessage))
      }

      def gotData(k: String, flags: Int, data: Array[Byte]) {
        assert(key == k, "Wrong key returned")

        if (data != null)
          result.tryComplete(Try {
            val tc = transcoder.asInstanceOf[Transcoder[T]]
            val value = tc.synchronized {
              tc.decode(new CachedData(flags, data, tc.getMaxSize))
            }
            SuccessfulResult(key, Option(value))
          })
        else
          result.tryComplete(Success(SuccessfulResult(key, None)))
      }

      def complete() {
        result.completePromise(key, promise)
      }
    })

    mconn.enqueueOperation(key, op)
    prepareFuture(key, op, promise, timeout)
  }

  def realAsyncSet[T](key: String, value: T, exp: Duration, timeout: FiniteDuration): Future[Result[Long]] = {
    val tc = transcoder.asInstanceOf[Transcoder[T]]
    val co: CachedData = tc.encode(value)
    val promise = Promise[Result[Long]]()
    val result = new MutablePartialResult[Long]

    val op: Operation = opFact.store(StoreType.set, key, co.getFlags, expiryToSeconds(exp).toInt, co.getData, new StoreOperation.Callback {
      def receivedStatus(opStatus: OperationStatus) {
        if (statusTranslation.isDefinedAt(opStatus))
          statusTranslation(opStatus) match {
            case CASSuccessStatus =>
              // nothing
            case failure =>
              result.tryComplete(Success(FailedResult(key, failure)))
          }

        else
          throw new UnhandledStatusException(
            "%s(%s)".format(opStatus.getClass, opStatus.getMessage))
      }

      def gotData(key: String, cas: Long) {
        result.tryComplete(Success(SuccessfulResult(key, cas)))
      }

      def complete() {
        result.completePromise(key, promise)
      }
    })

    mconn.enqueueOperation(key, op)
    prepareFuture(key, op, promise, timeout)
  }

  def realAsyncAdd[T](key: String, value: T, exp: Duration, timeout: FiniteDuration): Future[Result[Option[Long]]] = {
    val tc = transcoder.asInstanceOf[Transcoder[T]]
    val co: CachedData = tc.encode(value)
    val promise = Promise[Result[Option[Long]]]()
    val result = new MutablePartialResult[Option[Long]]

    val op: Operation = opFact.store(StoreType.add, key, co.getFlags, expiryToSeconds(exp).toInt, co.getData, new StoreOperation.Callback {
      def receivedStatus(opStatus: OperationStatus) {
        if (statusTranslation.isDefinedAt(opStatus))
          statusTranslation(opStatus) match {
            case CASExistsStatus =>
              result.tryComplete(Success(SuccessfulResult(key, None)))
            case CASSuccessStatus =>
              // nothing
            case failure =>
              result.tryComplete(Success(FailedResult(key, failure)))
          }

        else
          throw new UnhandledStatusException(
            "%s(%s)".format(opStatus.getClass, opStatus.getMessage))
      }

      def gotData(key: String, cas: Long) {
        result.tryComplete(Success(SuccessfulResult(key, Some(cas))))
      }

      def complete() {
        result.completePromise(key, promise)
      }
    })

    mconn.enqueueOperation(key, op)
    prepareFuture(key, op, promise, timeout)
  }

  def realAsyncDelete(key: String, timeout: FiniteDuration): Future[Result[Boolean]] = {
    val promise = Promise[Result[Boolean]]()
    val result = new MutablePartialResult[Boolean]

    val op = opFact.delete(key, new OperationCallback {
      def receivedStatus(opStatus: OperationStatus) {
        if (statusTranslation.isDefinedAt(opStatus))
          statusTranslation(opStatus) match {
            case CASSuccessStatus =>
              result.tryComplete(Success(SuccessfulResult(key, true)))
            case CASNotFoundStatus =>
              result.tryComplete(Success(SuccessfulResult(key, false)))
            case failure =>
              result.tryComplete(Success(FailedResult(key, failure)))
          }

        else
          throw new UnhandledStatusException(
            "%s(%s)".format(opStatus.getClass, opStatus.getMessage))
      }

      def complete() {
        result.completePromise(key, promise)
      }
    })

    mconn.enqueueOperation(key, op)
    prepareFuture(key, op, promise, timeout)
  }

  def realAsyncGets[T](key: String, timeout: FiniteDuration): Future[Result[Option[(T, Long)]]] = {
    val promise = Promise[Result[Option[(T, Long)]]]()
    val result = new MutablePartialResult[Option[(T, Long)]]

    val op: Operation = opFact.gets(key, new GetsOperation.Callback {
      def receivedStatus(opStatus: OperationStatus) {
        if (statusTranslation.isDefinedAt(opStatus))
          statusTranslation(opStatus) match {
            case CASNotFoundStatus =>
              result.tryComplete(Success(SuccessfulResult(key, None)))
            case CASSuccessStatus =>
              // nothing
            case failure =>
              result.tryComplete(Success(FailedResult(key, failure)))
          }

        else
          throw new UnhandledStatusException(
            "%s(%s)".format(opStatus.getClass, opStatus.getMessage))
      }

      def gotData(receivedKey: String, flags: Int, cas: Long, data: Array[Byte]) {
        assert(key == receivedKey, "Wrong key returned")
        assert(cas > 0, "CAS was less than zero:  " + cas)
        assert(!promise.isCompleted, "promise is already complete")

        result.tryComplete(Try {
          val tc = transcoder.asInstanceOf[Transcoder[T]]
          val value = tc.synchronized {
            Option(tc.decode(new CachedData(flags, data, tc.getMaxSize)))
          }
          SuccessfulResult(key, value.map(v => (v, cas)))
        })
      }

      def complete() {
        result.completePromise(key, promise)
      }
    })

    mconn.enqueueOperation(key, op)
    prepareFuture(key, op, promise, timeout)
  }

  def realAsyncCAS[T](key: String, casID: Long, value: T, exp: Duration, timeout: FiniteDuration): Future[Result[Boolean]] = {
    val tc = transcoder.asInstanceOf[Transcoder[T]]
    val co: CachedData = tc.encode(value)
    val promise = Promise[Result[Boolean]]()
    val result = new MutablePartialResult[Boolean]

    val op = opFact.cas(StoreType.set, key, casID, co.getFlags, expiryToSeconds(exp).toInt, co.getData, new StoreOperation.Callback {
      def receivedStatus(opStatus: OperationStatus) {
        if (statusTranslation.isDefinedAt(opStatus))
          statusTranslation(opStatus) match {
            case CASSuccessStatus =>
              result.tryComplete(Success(SuccessfulResult(key, true)))
            case CASExistsStatus =>
              result.tryComplete(Success(SuccessfulResult(key, false)))
            case CASNotFoundStatus =>
              result.tryComplete(Success(SuccessfulResult(key, false)))
            case failure =>
              result.tryComplete(Success(FailedResult(key, failure)))
          }

        else
          throw new UnhandledStatusException(
            "%s(%s)".format(opStatus.getClass, opStatus.getMessage))
      }

      def gotData(k: String, cas: Long) {
        assert(key == k, "Wrong key returned")
      }

      def complete() {
        result.completePromise(key, promise)
      }
    })

    mconn.enqueueOperation(key, op)
    prepareFuture(key, op, promise, timeout)
  }

  private[this] def statusTranslation: PartialFunction[OperationStatus, Status] = {
    case _: CancelledOperationStatus =>
      CancelledStatus
    case _: TimedOutOperationStatus =>
      TimedOutStatus
    case status: CASOperationStatus =>
      status.getCASResponse match {
        case CASResponse.EXISTS =>
          CASExistsStatus
        case CASResponse.NOT_FOUND =>
          CASNotFoundStatus
        case CASResponse.OK =>
          CASSuccessStatus
      }
  }

  private[this] def expiryToSeconds(duration: Duration) = duration match {
    case finite: FiniteDuration =>
      val seconds = finite.toSeconds
      if (seconds < 60 * 60 * 24 * 30)
        seconds
      else
        (System.currentTimeMillis() / 1000) + seconds

    // infinite duration (set to 365 days)
    case _ =>
      (System.currentTimeMillis() / 1000) + 31536000 // 60 * 60 * 24 * 365 -> 365 days in seconds
  }

  private[this] def prepareFuture[T](key: String, op: Operation, promise: Promise[Result[T]], atMost: FiniteDuration): Future[Result[T]] = {
    val future = promise.futureWithTimeout(atMost) {
      if (op.hasErrored)
        Failure(op.getException)
      else if (op.isCancelled)
        Success(FailedResult(key, CancelledStatus))
      else
        Success(FailedResult(key, TimedOutStatus))
    }

    future.onComplete {
      case Success(FailedResult(_, TimedOutStatus)) =>
        MemcachedConnection.opTimedOut(op)
        op.timeOut()
      case _ =>
        MemcachedConnection.opSucceeded(op)
    }

    future
  }
}
