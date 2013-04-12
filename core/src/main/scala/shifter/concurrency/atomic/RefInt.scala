package shifter.concurrency.atomic

import java.util.concurrent.atomic.AtomicInteger

final class RefInt(initialValue: Int) extends Ref[Int] {
  def set(update: Int) {
    instance.set(update)
  }

  def lazySet(update: Int) {
    instance.lazySet(update)
  }

  def get: Int =
    instance.get()

  def getAndSet(update: Int): Int = {
    instance.getAndSet(update)
  }

  def compareAndSet(expect: Int, update: Int): Boolean = {
    instance.compareAndSet(expect, update)
  }

  def weakCompareAndSet(expect: Int, update: Int) = {
    instance.weakCompareAndSet(expect, update)
  }

  override def increment(implicit num: Numeric[Int]) {
    instance.incrementAndGet()
  }

  override def incrementAndGet(implicit num: Numeric[Int]): Int =
    instance.incrementAndGet()

  override def getAndIncrement(implicit num: Numeric[Int]): Int =
    instance.getAndIncrement

  override def decrement(implicit num: Numeric[Int]) {
    instance.decrementAndGet()
  }

  override def decrementAndGet(implicit num: Numeric[Int]): Int =
    instance.decrementAndGet()

  override def getAndDecrement(implicit num: Numeric[Int]): Int =
    instance.getAndDecrement


  private[this] val instance = new AtomicInteger(initialValue)
}
