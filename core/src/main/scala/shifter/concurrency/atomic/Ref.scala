package shifter.concurrency.atomic

import java.util.concurrent.atomic.AtomicReference
import annotation.tailrec

final class Ref[T] private[atomic] (initialValue: T) {
  if (initialValue == null) throw null
  private val serialVersionUID: Long = -18385889765843242L

  @tailrec
  def transformAndExtract[U](cb: T => (T, U)): U = {
    val value = instance.get()
    val (newValue, extract) = cb(value)

    if (!compareAndSet(value, newValue))
      transformAndExtract(cb)
    else
      extract
  }

  @tailrec
  def transformAndGet(cb: T => T): T = {
    val value = instance.get()
    val newValue = cb(value)

    if (!compareAndSet(value, newValue))
      transformAndGet(cb)
    else
      newValue
  }

  @tailrec
  def getAndTransform(cb: T => T): T = {
    val value = instance.get()
    val update = cb(value)

    if (!compareAndSet(value, update))
      getAndTransform(cb)
    else
      value
  }

  @tailrec
  def transform(cb: T => T) {
    val value = instance.get()
    val update = cb(value)

    if (!compareAndSet(value, update))
      transform(cb)
  }

  def set(update: T) {
    if (update == null) throw null
    instance.set(update)
  }

  def lazySet(update: T) {
    if (update == null) throw null
    instance.lazySet(update)
  }

  def apply(): T = get

  def get: T =
    instance.get()

  def getAndSet(update: T): T = {
    if (update == null) throw null
    instance.getAndSet(update)
  }

  def compareAndSet(expect: T, update: T): Boolean = {
    if (update == null) throw null
    instance.compareAndSet(expect, update)
  }

  def weakCompareAndSet(expect: T, update: T) = {
    if (update == null) throw null
    instance.weakCompareAndSet(expect, update)
  }

  private[this] val instance =
    new AtomicReference[T](initialValue)

  override def hashCode(): Int =
    get.hashCode()

  override def equals(obj: Any): Boolean =
    obj match {
      case other: Ref[_] =>
        other.get == this.get
      case _ =>
        false
    }
}

object Ref {
  def apply[T](value: T) =
    new Ref[T](value)

  def unnapply[T](ref: Ref[T]): Option[T] =
    Some(ref.get)
}
