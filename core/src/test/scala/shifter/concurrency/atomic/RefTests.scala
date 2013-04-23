package shifter.concurrency.atomic

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import shifter.concurrency.atomic.Ref
import collection.concurrent.RDCSS_Descriptor

@RunWith(classOf[JUnitRunner])
class RefTests extends FunSuite {

  test("transform") {
    val initialValue = 2
    val r = Ref(initialValue)
    val transformation: Int => Int = _ * 2
    val retValue = r.transform(transformation)

    assert(r.get === transformation(initialValue))
    assert(retValue === true)
  }

  test("transform returns false when old value equals new one") {
    val initialValue = 2
    val r = Ref(initialValue)
    val transformation: Int => Int = _ * 1
    val retValue = r.transform(transformation)

    assert(r.get === transformation(initialValue))
    assert(retValue === false)
  }

  test("transformAndGet") {
    val initialValue = 2
    val r = Ref(initialValue)
    val transformation: Int => Int = _ * 2
    val result = r.transformAndGet(transformation)

    assert(result === transformation(initialValue))
    assert(r.get === transformation(initialValue))
  }

  test("getAndTransform") {
    val initialValue = 3
    val r = Ref(initialValue)
    val transformation: Int => Int = _ * 2
    val result = r.getAndTransform(transformation)

    assert(result === initialValue)
    assert(r.get === transformation(initialValue))
  }

  test("transformAndExtract") {
    val initialValue = -3
    val r = Ref(initialValue)
    val expectedResult = "something"
    def transformation(x: Int): (Int, String) = (x * 2, expectedResult)

    val actualResult = r.transformAndExtract(transformation)

    assert(actualResult === expectedResult)
  }

  test("increment") {
    val initialValue = BigInt(2)
    val r = Ref(initialValue)
    r.increment

    assert(r.get === initialValue + BigInt(1))
  }

  test("incrementAndGet") {
    val initialValue = BigInt(2)
    val r = Ref(initialValue)
    val newValue = r.incrementAndGet

    assert(newValue === initialValue + BigInt(1))
    assert(r.get === initialValue + BigInt(1))
  }

  test("getAndIncrement") {
    val initialValue = BigInt(2)
    val r = Ref(initialValue)
    val result = r.getAndIncrement

    assert(result === initialValue)
    assert(r.get === initialValue + BigInt(1))
  }

  test("decrement") {
    val initialValue = BigInt(2)
    val r = Ref(initialValue)
    r.decrement

    assert(r.get === initialValue - BigInt(1))
  }

  test("decrementAndGet") {
    val initialValue = BigInt(2)
    val r = Ref(initialValue)
    val newValue = r.decrementAndGet

    assert(newValue === initialValue - BigInt(1))
    assert(r.get === initialValue - BigInt(1))
  }

  test("getAndDecrement") {
    val initialValue = BigInt(2)
    val r = Ref(initialValue)
    val newValue = r.getAndDecrement

    assert(newValue === initialValue)
    assert(r.get === initialValue - BigInt(1))
  }
}