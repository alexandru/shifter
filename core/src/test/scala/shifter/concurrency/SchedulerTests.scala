package shifter.concurrency

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import concurrent.ExecutionContext.Implicits.global
import concurrent.duration._
import org.scalatest.concurrent.AsyncAssertions.Waiter
import concurrent.Await

/*
 * Note: This tests are fragile
 * because they make assumptions about how much it will take until a certain statement gets executed.
 */
@RunWith(classOf[JUnitRunner])
class SchedulerTests extends FunSuite {

  test("runOnce") {
    val w = new Waiter()

    val task = scheduler.runOnce(10) {
      w.dismiss()
    }

    w.await()
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