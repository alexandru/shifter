package shifter.concurrency.atomic

import scala.annotation.tailrec

trait Ref[@specialized(scala.Int, scala.Long, scala.Boolean) T] {
  /**
    * @return The value stored in this `Ref`
    */
  def get: T

  /**
    * Updates the value stored in this `Ref` to a new value
    */
  def set(update: T)

  /**
    *
    */
  def compareAndSet(expect: T, update: T): Boolean
  def getAndSet(update: T): T

  final def apply(): T = get

  /**
    * Adds one to the value stored in this `Ref`.
    */
  def increment(implicit num : Numeric[T]) {
    transform(x => num.plus(x, num.one))
  }

  /**
    * Substracts one from the value stored in this `Ref`.
    */
  def decrement(implicit num : Numeric[T]) {
    transform(x => num.minus(x, num.one))
  }

  /**
    * Adds one to the value stored in this `Ref` and returns the '''new''' value.
    *
    * @return The value stored in this Ref '''after''' adding one.
    */
  def incrementAndGet(implicit num : Numeric[T]) =
    transformAndGet(x => num.plus(x, num.one))

  /**
    * Substracts one from the value stored in this `Ref` and returns the '''new''' value.
    *
    * @return The value stored in this `Ref` '''after''' subtraction.
    */
  def decrementAndGet(implicit num : Numeric[T]) =
    transformAndGet(x => num.minus(x, num.one))

  /**
    * Adds one to the value stored in this `Ref` and returns the '''old''' value.
    *
    * @return The value stored in this Ref '''before''' adding one.
    */
  def getAndIncrement(implicit num : Numeric[T]) =
    getAndTransform(x => num.plus(x, num.one))

  /**
    * Adds one to the value stored in this `Ref` and returns the '''old''' value.
    *
    * @return The value stored in this Ref '''before''' subtraction.
    */
  def getAndDecrement(implicit num : Numeric[T]) =
    getAndTransform(x => num.minus(x, num.one))

  /**
    * Applies a function that returns a pair to the current value of this `Ref`.
    * The first element of the returned pair becomes the new value stored in this `Ref`.
    * The second element is returned.
    *
    * @return The second element of the pair returned by the applying `cb` to the current value in this `Ref`.
    */
  @tailrec
  final def transformAndExtract[U](cb: T => (T, U)): U = {
    val value = get
    val (newValue, extract) = cb(value)

    if (!compareAndSet(value, newValue))
      transformAndExtract(cb)
    else
      extract
  }

  /**
    * Transforms the value stored in this `Ref` by applying `cb`.
    * @param cb
    * @return The new value stored in this `Ref` (the result of `cb` applied to value stored in this `Ref`).
    */
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
