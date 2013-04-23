package shifter.concurrency.atomic

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import shifter.concurrency.atomic.Ref

@RunWith(classOf[JUnitRunner])
class RefAnyTests extends FunSuite {

  test("get returns correct value") {
    val initialValue = new String("InitialValue")
    val r = Ref(initialValue)

    assert(r.get === initialValue)
  }

  test("set updates value correctly") {
    val initialValue = new String("InitialValue")
    val r = Ref(initialValue)
    val updatedValue = "NewValue"
    r.set(updatedValue)

    assert(r.get === updatedValue)
  }

  test("getAndSet updates value and returns previous one") {
    val initialValue = new String("InitialValue")
    val r = Ref(initialValue)

    val updatedValue = new String("NewValue")
    val result = r.getAndSet(updatedValue)

    assert(result === initialValue)
    assert(r.get === updatedValue)
  }

  test("compareAndSet changes value on condition met") {
    val initialValue = new String("InitialValue")
    val r = Ref(initialValue)
    val updatedValue = new String("NewValue")

    val success = r.compareAndSet(initialValue, updatedValue)

    assert(r.get === updatedValue)
    assert(success)
  }

  test("compareAndSet - value stays the same on condition not met") {
    val initialValue = new String("InitialValue")
    val r = Ref(initialValue)

    val success = r.compareAndSet(new String("OtherValue"), new String("NewValue"))

    assert(r.get === initialValue)
    assert(!success)
  }
}