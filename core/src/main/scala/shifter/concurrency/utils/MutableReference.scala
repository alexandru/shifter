package shifter.concurrency.utils

import annotation.tailrec
import java.util.concurrent.atomic.AtomicReference

final class MutableReference[T] {
  @tailrec
  def get: Option[T] = state.get() match {
    case Borrowed => get
    case NotInitialized => None
    case WithValue(value) => Some(value)
  }

  @tailrec
  def set(newValue: Option[T]) {
    state.get() match {
      case ref @ (NotInitialized | WithValue(_)) if state.compareAndSet(ref, Borrowed) =>
        newValue match {
          case None => state.set(NotInitialized)
          case Some(value) => state.set(WithValue(value))
        }
      case _ =>
        set(newValue)
    }
  }

  @tailrec
  def getAndTransform(cb: Option[T] => Option[T]): Option[T] =
    state.get() match {
      case Borrowed =>
        getAndTransform(cb)

      case NotInitialized =>
        if (!state.compareAndSet(NotInitialized, Borrowed))
          getAndTransform(cb)
        else
          try {
            cb(None) match {
              case None =>
                state.set(NotInitialized)
              case ref @ Some(value) =>
                state.set(WithValue(value))
            }
            None
          }
          catch {
            case ex: Throwable =>
              state.set(NotInitialized)
              throw ex
          }

      case ref @ WithValue(value) =>
        if (!state.compareAndSet(ref, Borrowed))
          getAndTransform(cb)
        else
          try {
            cb(Some(value)) match {
              case None =>
                state.set(NotInitialized)
              case ref @ Some(newValue) =>
                state.set(WithValue(newValue))
            }
            Some(value)
          }
          catch {
            case ex: Throwable =>
              state.set(ref)
              throw ex
          }
    }

  @tailrec
  def transformAndGet(cb: Option[T] => Option[T]): Option[T] =
    state.get() match {
      case Borrowed =>
        transformAndGet(cb)

      case NotInitialized =>
        if (!state.compareAndSet(NotInitialized, Borrowed))
          transformAndGet(cb)
        else
          try {
            cb(None) match {
              case None =>
                state.set(NotInitialized)
                None
              case ref @ Some(value) =>
                state.set(WithValue(value))
                ref
            }
          }
          catch {
            case ex: Throwable =>
              state.set(NotInitialized)
              throw ex
          }

      case ref @ WithValue(value) =>
        if (!state.compareAndSet(ref, Borrowed))
          transformAndGet(cb)
        else
          try {
            cb(Some(value)) match {
              case None =>
                state.set(NotInitialized)
                None
              case ref @ Some(newValue) =>
                state.set(WithValue(newValue))
                ref
            }
          }
          catch {
            case ex: Throwable =>
              state.set(ref)
              throw ex
          }
    }

  private[this] sealed trait RefState
  private[this] case object NotInitialized extends RefState
  private[this] case object Borrowed extends RefState
  private[this] case class WithValue(value: T) extends RefState

  private[this] val state = new AtomicReference(NotInitialized : RefState)
}
