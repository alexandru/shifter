package shifter.concurrency

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import shifter.concurrency.atomic.Ref

@RunWith(classOf[JUnitRunner])
class RefBooleanTests extends FunSuite {

  test("get returns correct value") {
    val falseRef = Ref(false)
    val trueRef = Ref(true)

    assert(trueRef.get === true)
    assert(falseRef.get === false)
  }

  test("set updates value correctly") {
    val initialValue = false
    val r = Ref(initialValue)
    val updatedValue = !initialValue
    r.set(updatedValue)

    assert(r.get === updatedValue)
  }

  test("getAndSet updates value and returns previous one") {
    val initialValue = false
    val r = Ref(initialValue)

    val updatedValue = true
    val result = r.getAndSet(updatedValue)

    assert(result === initialValue)
    assert(r.get === updatedValue)
  }

  test("compareAndSet changes value on condition met") {
    val initialValue = false
    val r = Ref(initialValue)
    val updatedValue = !initialValue

    val success = r.compareAndSet(initialValue, updatedValue)

    assert(r.get === updatedValue)
    assert(success)
  }

  test("compareAndSet - value stays the same on condition not met") {
    val initialValue = false
    val r = Ref(initialValue)

    val success = r.compareAndSet(!initialValue, !initialValue)

    assert(r.get === initialValue)
    assert(!success)
  }

}