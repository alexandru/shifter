package shifter.concurrency.atomic

import scala.annotation.tailrec
import scala.reflect.ClassTag

trait Ref[@specialized(scala.Int, scala.Long, scala.Boolean) T] {
  def get: T
  def set(update: T)
  def lazySet(update: T)
  def compareAndSet(expect: T, update: T): Boolean
  def weakCompareAndSet(expect: T, update: T): Boolean
  def getAndSet(update: T): T

  final def apply(): T = get

  def increment(implicit num : Numeric[T]) {
    transform(x => num.plus(x, num.one))
  }

  def decrement(implicit num : Numeric[T]) {
    transform(x => num.minus(x, num.one))
  }

  def incrementAndGet(implicit num : Numeric[T]) =
    transformAndGet(x => num.plus(x, num.one))

  def decrementAndGet(implicit num : Numeric[T]) =
    transformAndGet(x => num.minus(x, num.one))

  def getAndIncrement(implicit num : Numeric[T]) =
    getAndTransform(x => num.plus(x, num.one))

  def getAndDecrement(implicit num : Numeric[T]) =
    getAndTransform(x => num.minus(x, num.one))

  @tailrec
  final def transformAndExtract[U](cb: T => (T, U)): U = {
    val value = get
    val (newValue, extract) = cb(value)

    if (!compareAndSet(value, newValue))
      transformAndExtract(cb)
    else
      extract
  }

  @tailrec
  final def transformAndGet(cb: T => T): T = {
    val value = get
    val newValue = cb(value)

    if (!compareAndSet(value, newValue))
      transformAndGet(cb)
    else
      newValue
  }

  @tailrec
  final def getAndTransform(cb: T => T): T = {
    val value = get
    val update = cb(value)

    if (!compareAndSet(value, update))
      getAndTransform(cb)
    else
      value
  }

  @tailrec
  final def transform(cb: T => T): Boolean = {
    val value = get
    val update = cb(value)

    if (!compareAndSet(value, update))
      transform(cb)
    else
      value != update
  }

  override def toString: String =
    "Ref(" + get.toString + ")"
}

object Ref {
  def apply(initialValue: Int): RefInt =
    new RefInt(initialValue)

  def apply(initialValue: Long): RefLong =
    new RefLong(initialValue)

  def apply(initialValue: Boolean): RefBoolean =
    new RefBoolean(initialValue)

  def apply[T](initialValue: T): Ref[T] =
    new RefAny[T](initialValue)
}
