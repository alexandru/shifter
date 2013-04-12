package shifter.concurrency.atomic

import java.util.concurrent.atomic.AtomicBoolean

final class RefBoolean private[atomic] (initialValue: Boolean) extends Ref[Boolean] {
  def set(update: Boolean) {
    instance.set(update)
  }

  def lazySet(update: Boolean) {
    instance.lazySet(update)
  }

  def get: Boolean =
    instance.get()

  def getAndSet(update: Boolean): Boolean = {
    instance.getAndSet(update)
  }

  def compareAndSet(expect: Boolean, update: Boolean): Boolean = {
    instance.compareAndSet(expect, update)
  }

  def weakCompareAndSet(expect: Boolean, update: Boolean) = {
    instance.weakCompareAndSet(expect, update)
  }

  private[this] val instance = new AtomicBoolean(initialValue)
}
