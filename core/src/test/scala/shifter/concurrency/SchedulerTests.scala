package shifter.concurrency

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import concurrent.ExecutionContext.Implicits.global
import concurrent.duration._
import concurrent.Await

/*
 * Note: This tests are fragile
 * because they make assumptions about how much it will take until a certain statement gets executed.
 */
@RunWith(classOf[JUnitRunner])
class SchedulerTests extends FunSuite {

  test("runOnce") {
    val ref = shifter.concurrency.atomic.Ref(0L)
    val startAt = System.currentTimeMillis()

    scheduler.runOnce(100) {
      ref.set(System.currentTimeMillis() - startAt)
      ref.synchronized { ref.notify() }
    }

    Thread.sleep(50)
    while (ref.get == 0 && System.currentTimeMillis() - startAt < 1000)
      ref.synchronized { ref.wait(100) }

    assert(ref.get >= 100, "Wrong time: " + ref.get.toString)
  }

  test("cancel") {
    val task = scheduler.runOnce(500) {
      fail("Canceled task executed")
    }

    scheduler.cancel(task)
    Thread.sleep(1000)
  }

  test("future") {
    val retrievedValue = "task ran"
    val future = scheduler.future(500.millis) { retrievedValue }
    val result = Await.result(future, 1.second)

    assert(result === retrievedValue)
  }
}