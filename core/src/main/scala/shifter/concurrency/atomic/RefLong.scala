package shifter.concurrency.atomic

import java.util.concurrent.atomic.AtomicLong

final class RefLong private[atomic] (initialValue: Long) extends Ref[Long] {
  def set(update: Long) {
    instance.set(update)
  }

  def get: Long =
    instance.get()

  def getAndSet(update: Long): Long = {
    instance.getAndSet(update)
  }

  def compareAndSet(expect: Long, update: Long): Boolean = {
    instance.compareAndSet(expect, update)
  }

  override def increment(implicit num: Numeric[Long]) {
    instance.incrementAndGet()
  }

  override def incrementAndGet(implicit num: Numeric[Long]): Long =
    instance.incrementAndGet()

  override def getAndIncrement(implicit num: Numeric[Long]): Long =
    instance.getAndIncrement

  override def decrement(implicit num: Numeric[Long]) {
    instance.decrementAndGet()
  }

  override def decrementAndGet(implicit num: Numeric[Long]): Long =
    instance.decrementAndGet()

  override def getAndDecrement(implicit num: Numeric[Long]): Long =
    instance.getAndDecrement

  private[this] val instance = new AtomicLong(initialValue)
}
