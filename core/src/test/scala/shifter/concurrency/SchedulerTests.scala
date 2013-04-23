package shifter.concurrency

import atomic.Ref
import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import concurrent.ExecutionContext.Implicits.global
import concurrent.duration._
import org.scalatest.concurrent.AsyncAssertions.Waiter
import org.scalatest.concurrent.AsyncAssertions.dismissals

/*
 * Note: This tests are fragile
 * because they make assumptions on how much it will take until a certain statement gets executed.
 */
@RunWith(classOf[JUnitRunner])
class SchedulerTests extends FunSuite {

  test("cancel") {
    val task = scheduler.runOnce(500) {
      fail("Canceled task executed")
    }

    scheduler.cancel(task)
    Thread.sleep(1000)
  }

  test("runOnce") {
    val w = new Waiter()

    val task = scheduler.runOnce(10) {
      w.dismiss()
    }

    w.await()
  }


}