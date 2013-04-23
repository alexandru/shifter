package shifter.concurrency

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import shifter.concurrency.atomic.Ref

@RunWith(classOf[JUnitRunner])
class RefLongTests extends FunSuite {

   test("get returns correct value") {
     val initialValue = 2.toLong
     val r = Ref(initialValue)

     assert(r.get === initialValue)
   }

   test("set updates value correctly") {
     val initialValue = 2.toLong
     val r = Ref(initialValue)
     val updatedValue = 3.toLong
     r.set(updatedValue)

     assert(r.get === updatedValue)
   }

   test("getAndSet updates value and returns previous one") {
     val initialValue = 2.toLong
     val r = Ref(initialValue)

     val updatedValue = 5.toLong
     val result = r.getAndSet(updatedValue)

     assert(result === initialValue)
     assert(r.get === updatedValue)
   }

   test("compareAndSet changes value on condition met") {
     val initialValue = 2.toLong
     val r = Ref(initialValue)
     val updatedValue = 3.toLong

     val success = r.compareAndSet(initialValue, updatedValue)

     assert(r.get === updatedValue)
     assert(success)
   }

   test("compareAndSet - value stays the same on condition not met") {
     val initialValue = 2.toLong
     val r = Ref(initialValue)

     val success = r.compareAndSet(3.toLong, 5.toLong)

     assert(r.get === initialValue)
     assert(!success)
   }

   test("increment") {
     val initialValue = 2.toLong
     val r = Ref(initialValue)
     r.increment

     assert(r.get === initialValue + 1.toLong)
   }

   test("incrementAndGet") {
     val initialValue = 2.toLong
     val r = Ref(initialValue)
     val newValue = r.incrementAndGet

     assert(newValue === initialValue + 1.toLong)
     assert(r.get === initialValue + 1.toLong)
   }

   test("getAndIncrement") {
     val initialValue = 2.toLong
     val r = Ref(initialValue)
     val result = r.getAndIncrement

     assert(result === initialValue)
     assert(r.get === initialValue + 1.toLong)
   }

   test("decrement") {
     val initialValue = 2.toLong
     val r = Ref(initialValue)
     r.decrement

     assert(r.get === initialValue - 1.toLong)
   }

   test("decrementAndGet") {
     val initialValue = 2.toLong
     val r = Ref(initialValue)
     val newValue = r.decrementAndGet

     assert(newValue === initialValue - 1.toLong)
     assert(r.get === initialValue - 1.toLong)
   }

   test("getAndDecrement") {
     val initialValue = 2.toLong
     val r = Ref(initialValue)
     val newValue = r.getAndDecrement

     assert(newValue === initialValue)
     assert(r.get === initialValue - 1.toLong)
   }

 }