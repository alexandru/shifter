package shifter.concurrency.atomic

import java.util.concurrent.atomic.AtomicReference

final class RefAny[T] private[atomic] (initialValue: T) extends Ref[T] {

  def set(update: T) {
    instance.set(update)
  }

  def lazySet(update: T) {
    instance.lazySet(update)
  }

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

