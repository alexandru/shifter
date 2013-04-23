package shifter.concurrency.atomic

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import shifter.concurrency.atomic.Ref

@RunWith(classOf[JUnitRunner])
class
RefIntTests extends FunSuite {

  test("get returns correct value") {
    val initialValue = 2
    val r = Ref(initialValue)

    assert(r.get === initialValue)
  }

  test("set updates value correctly") {
    val initialValue = 2
    val r = Ref(initialValue)
    val updatedValue = 3
    r.set(updatedValue)

    assert(r.get === updatedValue)
  }

  test("getAndSet updates value and returns previous one") {
    val initialValue = 2
    val r = Ref(initialValue)

    val updatedValue = 5
    val result = r.getAndSet(updatedValue)

    assert(result === initialValue)
    assert(r.get === updatedValue)
  }

  test("compareAndSet changes value on condition met") {
    val initialValue = 2
    val r = Ref(initialValue)
    val updatedValue = 3

    val success = r.compareAndSet(initialValue, updatedValue)

    assert(r.get === updatedValue)
    assert(success)
  }

  test("compareAndSet - value stays the same on condition not met") {
    val initialValue = 2
    val r = Ref(initialValue)

    val success = r.compareAndSet(3, 5)

    assert(r.get === initialValue)
    assert(!success)
  }

  test("increment") {
    val initialValue = 2
    val r = Ref(initialValue)
    r.increment

    assert(r.get === initialValue + 1)
  }

  test("incrementAndGet") {
    val initialValue = 2
    val r = Ref(initialValue)
    val newValue = r.incrementAndGet

    assert(newValue === initialValue + 1)
    assert(r.get === initialValue + 1)
  }

  test("getAndIncrement") {
    val initialValue = 2
    val r = Ref(initialValue)
    val result = r.getAndIncrement

    assert(result === initialValue)
    assert(r.get === initialValue + 1)
  }

  test("decrement") {
    val initialValue = 2
    val r = Ref(initialValue)
    r.decrement

    assert(r.get === initialValue - 1)
  }

  test("decrementAndGet") {
    val initialValue = 2
    val r = Ref(initialValue)
    val newValue = r.decrementAndGet

    assert(newValue === initialValue - 1)
    assert(r.get === initialValue - 1)
  }

  test("getAndDecrement") {
    val initialValue = 2
    val r = Ref(initialValue)
    val newValue = r.getAndDecrement

    assert(newValue === initialValue)
    assert(r.get === initialValue - 1)
  }

}