package shifter.cache.memcached.internals

import net.spy.memcached.{CASResponse, CachedData, MemcachedClient, ConnectionFactory}
import java.{util => jutil}
import java.net.InetSocketAddress
import concurrent.{ExecutionContext, Promise, Future}
import net.spy.memcached.transcoders.Transcoder
import net.spy.memcached.ops._
import scala.util.Success
import concurrent.duration.{FiniteDuration, Duration}
import shifter.cache.UnhandledStatusException


class SpyMemcachedClient(conn: ConnectionFactory, addresses: jutil.List[InetSocketAddress])
    extends MemcachedClient(conn, addresses) {

  def realAsyncGet[T](key: String)(implicit ec: ExecutionContext): Future[Result[Option[T]]] = {
    val promise = Promise[Result[Option[T]]]()
    val tc = transcoder.asInstanceOf[Transcoder[T]]

    val op: Operation = opFact.get(key, new GetOperation.Callback {
      def receivedStatus(opStatus: OperationStatus) {
        if (statusTranslation.isDefinedAt(opStatus))
          statusTranslation(opStatus) match {
            case CASNotFoundStatus =>
              promise.complete(Success(SuccessfulResult(key, None)))
            case CASSuccessStatus =>
              // nothing
            case failure =>
              promise.complete(Success(FailedResult(key, failure)))
          }

        else
          promise.completeWith(Future {
            throw new UnhandledStatusException(
              "%s(%s)".format(opStatus.getClass, opStatus.getMessage))
          })
      }

      def gotData(k: String, flags: Int, data: Array[Byte]) {
        assert(key == k, "Wrong key returned")
        assert(!promise.isCompleted, "Wrong state, promise should not be completed")
        val value: T = tc.decode(new CachedData(flags, data, tc.getMaxSize))
        promise.complete(Success(SuccessfulResult(key, Some(value))))
      }

      def complete() {
        if (!promise.isCompleted)
          promise.complete(Success(SuccessfulResult(key, None)))
      }
    })

    mconn.enqueueOperation(key, op)
    promise.future
  }

  def realAsyncSet[T](key: String, value: T, exp: Duration)(implicit ec: ExecutionContext): Future[Result[Long]] = {
    val tc = transcoder.asInstanceOf[Transcoder[T]]
    val co: CachedData = tc.encode(value)
    val promise = Promise[Result[Long]]()

    val op: Operation = opFact.store(StoreType.set, key, co.getFlags, expiryToSeconds(exp).toInt, co.getData, new StoreOperation.Callback {
      def receivedStatus(opStatus: OperationStatus) {
        if (statusTranslation.isDefinedAt(opStatus))
          statusTranslation(opStatus) match {
            case CASSuccessStatus =>
              // nothing
            case failure =>
              promise.complete(Success(FailedResult(key, failure)))
          }

        else
          promise.completeWith(Future {
            throw new UnhandledStatusException(
              "%s(%s)".format(opStatus.getClass, opStatus.getMessage))
          })
      }

      def gotData(key: String, cas: Long) {
        promise.complete(Success(SuccessfulResult(key, cas)))
      }

      def complete() {
        if (!promise.isCompleted)
          throw new IllegalStateException("Promise should have been completed by now")
      }
    })

    mconn.enqueueOperation(key, op)
    promise.future
  }


  def realAsyncAdd[T](key: String, value: T, exp: Duration)(implicit ec: ExecutionContext): Future[Result[Option[Long]]] = {
    val tc = transcoder.asInstanceOf[Transcoder[T]]
    val co: CachedData = tc.encode(value)
    val promise = Promise[Result[Option[Long]]]()

    val op: Operation = opFact.store(StoreType.add, key, co.getFlags, expiryToSeconds(exp).toInt, co.getData, new StoreOperation.Callback {
      def receivedStatus(opStatus: OperationStatus) {
        if (statusTranslation.isDefinedAt(opStatus))
          statusTranslation(opStatus) match {
            case CASExistsStatus =>
              promise.complete(Success(SuccessfulResult(key, None)))
            case CASSuccessStatus =>
              // nothing
            case failure =>
              promise.complete(Success(FailedResult(key, failure)))
          }

        else
          promise.completeWith(Future {
            throw new UnhandledStatusException(
              "%s(%s)".format(opStatus.getClass, opStatus.getMessage))
          })
      }

      def gotData(key: String, cas: Long) {
        promise.complete(Success(SuccessfulResult(key, Some(cas))))
      }

      def complete() {
        if (!promise.isCompleted)
          promise.complete(Success(SuccessfulResult(key, None)))
      }
    })

    mconn.enqueueOperation(key, op)
    promise.future
  }

  def realDelete(key: String)(implicit ec: ExecutionContext): Future[Result[Boolean]] = {
    val promise = Promise[Result[Boolean]]()

    val op = opFact.delete(key, new OperationCallback {
      def receivedStatus(opStatus: OperationStatus) {
        if (statusTranslation.isDefinedAt(opStatus))
          statusTranslation(opStatus) match {
            case CASSuccessStatus =>
              promise.complete(Success(SuccessfulResult(key, true)))
            case CASNotFoundStatus =>
              promise.complete(Success(SuccessfulResult(key, false)))
            case failure =>
              promise.complete(Success(FailedResult(key, failure)))
          }

        else
          promise.completeWith(Future {
            throw new UnhandledStatusException(
              "%s(%s)".format(opStatus.getClass, opStatus.getMessage))
          })
      }

      def complete() {
        if (!promise.isCompleted)
          promise.complete(Success(SuccessfulResult(key, false)))
      }
    })

    mconn.enqueueOperation(key, op)
    promise.future
  }

  def realAsyncGets[T](key: String)(implicit ec: ExecutionContext): Future[Result[Option[(T, Long)]]] = {
    val promise = Promise[Result[Option[(T, Long)]]]()
    val tc = transcoder.asInstanceOf[Transcoder[T]]

    val op: Operation = opFact.gets(key, new GetsOperation.Callback {
      def receivedStatus(opStatus: OperationStatus) {
        if (statusTranslation.isDefinedAt(opStatus))
          statusTranslation(opStatus) match {
            case CASNotFoundStatus =>
              promise.complete(Success(SuccessfulResult(key, None)))
            case CASSuccessStatus =>
              // nothing
            case failure =>
              promise.complete(Success(FailedResult(key, failure)))
          }

        else
          promise.completeWith(Future {
            throw new UnhandledStatusException(
              "%s(%s)".format(opStatus.getClass, opStatus.getMessage))
          })
      }

      def gotData(receivedKey: String, flags: Int, cas: Long, data: Array[Byte]) {
        assert(key == receivedKey, "Wrong key returned")
        assert(cas > 0, "CAS was less than zero:  " + cas)
        assert(!promise.isCompleted, "promise is already complete")

        val value: T = tc.decode(new CachedData(flags, data, tc.getMaxSize))
        promise.complete(Success(SuccessfulResult(key, Some(value, cas))))
      }

      def complete() {
        assert(promise.isCompleted, "promise should have been complete")
      }
    })

    mconn.enqueueOperation(key, op)
    promise.future
  }

  def realAsyncCAS[T](key: String, casID: Long, value: T, exp: Duration)(implicit ec: ExecutionContext): Future[Result[Boolean]] = {
    val tc = transcoder.asInstanceOf[Transcoder[T]]
    val co: CachedData = tc.encode(value)
    val promise = Promise[Result[Boolean]]()

    val op = opFact.cas(StoreType.set, key, casID, co.getFlags, expiryToSeconds(exp).toInt, co.getData, new StoreOperation.Callback {
      def receivedStatus(opStatus: OperationStatus) {
        if (statusTranslation.isDefinedAt(opStatus))
          statusTranslation(opStatus) match {
            case CASSuccessStatus =>
              promise.complete(Success(SuccessfulResult(key, true)))
            case CASExistsStatus =>
              promise.complete(Success(SuccessfulResult(key, false)))
            case CASNotFoundStatus =>
              promise.complete(Success(SuccessfulResult(key, false)))
            case failure =>
              promise.complete(Success(FailedResult(key, failure)))
          }

        else
          promise.completeWith(Future {
            throw new UnhandledStatusException(
              "%s(%s)".format(opStatus.getClass, opStatus.getMessage))
          })
      }

      def gotData(key: String, cas: Long) {}

      def complete() {
        assert(promise.isCompleted, "promise should have been completed")
      }
    })

    mconn.enqueueOperation(key, op)
    promise.future
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
}
