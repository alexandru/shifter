package shifter.concurrency.utils

import java.util.concurrent.atomic.AtomicReference
import annotation.tailrec

final class Ref[T] private[utils] (initialValue: T) {
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
  def transform(cb: T => T): Boolean = {
    val value = instance.get()
    val update = cb(value)

    if (!compareAndSet(value, update))
      transform(cb)
    else
      value != update
  }

  def set(update: T) {
    instance.set(update)
  }

  def lazySet(update: T) {
    instance.lazySet(update)
  }

  def apply(): T = get

  def get: T =
    instance.get()

  def getAndSet(update: T): T = {
    instance.getAndSet(update)
  }

  def compareAndSet(expect: T, update: T): Boolean = {
    instance.compareAndSet(expect, update)
  }

  def weakCompareAndSet(expect: T, update: T) = {
    instance.weakCompareAndSet(expect, update)
  }

  private[this] val instance =
    new AtomicReference[T](initialValue)
}

object Ref {
  def apply[T](value: T) =
    new Ref[T](value)

  def unapply[T](ref: Ref[T]): Option[T] =
    Some(ref.get)
}
