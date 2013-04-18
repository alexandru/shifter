package shifter.concurrency

import collection.immutable.TreeMap
import annotation.tailrec
import scala.concurrent.{Promise, Future, ExecutionContext}
import util.control.NonFatal
import shifter.concurrency.atomic.Ref
import scala.util.Try
import scala.concurrent.duration.FiniteDuration


/**
 * Light-weight task scheduler, used for Futures/Promises timeouts.
 */
object scheduler {
  case class TaskKey(id: Long, runsAt: Long)

  private[this] case class TaskCallback(
    run: () => Any,
    ec: ExecutionContext
  )

  def future[T](initialDelay: FiniteDuration)(cb: => T)(implicit ec: ExecutionContext): Future[T] = {
    val promise = Promise[T]()
    runOnce(initialDelay.toMillis) {
      promise.completeWith(Future(cb))
    }
    promise.future
  }

  def runOnce(delayMillis: Long)(callback: => Any)(implicit ec: ExecutionContext): TaskKey = {
    val newID = lastID.incrementAndGet
    val task = TaskKey(newID, System.currentTimeMillis() + delayMillis)
    pushTask(task, TaskCallback(() => callback, ec))
    scheduledTasks.synchronized { scheduledTasks.notifyAll() }
    task
  }

  @tailrec
  def cancel(task: TaskKey): Boolean = {
    val tasks = scheduledTasks.get
    if (!tasks.contains(task))
      false
    else if (!scheduledTasks.compareAndSet(tasks, tasks - task))
      cancel(task)
    else
      true
  }

  @tailrec
  private[this] def pushTask(task: TaskKey, callback: TaskCallback) {
    val tasks = scheduledTasks.get
    if (!tasks.contains(task))
      if (!scheduledTasks.compareAndSet(tasks, tasks.updated(task, callback)))
        pushTask(task, callback)
  }

  @tailrec
  private[this] def waitOrExecute() {
    val tasks = scheduledTasks.get
    val firstTaskOpt = if (!tasks.isEmpty)
      Some(tasks.firstKey)
    else
      None

    val currentTime = getCurrentTime

    firstTaskOpt match {
      // we've got no tasks to worry about, so sleep for 500ms
      case None =>
        scheduledTasks.synchronized {
          scheduledTasks.wait(500)
        }
        // retry
        waitOrExecute()

      case Some(firstTask) =>
        // try executing the first task if it's time for it
        if (firstTask.runsAt <= currentTime) {
          // pulls the task out of the queue
          val newQueue = tasks - firstTask
          if (!scheduledTasks.compareAndSet(tasks, newQueue))
          // retry
            waitOrExecute()
          else {
            // we got it covered, so execute
            val callback = tasks(firstTask)
            val context = callback.ec
            val runnable = callback.run

            context.execute(new Runnable {
              def run() {
                try {
                  runnable.apply()
                }
                catch {
                  case NonFatal(ex) =>
                    context.reportFailure(ex)
                }
              }
            })

            // tail rec
            waitOrExecute()
          }
        }
        // task is not ready yet, so we need to sleep
        else {
          val millis = firstTask.runsAt - System.currentTimeMillis()
          val sleepFor = if (millis > 500) 500 else millis
          if (sleepFor > 0)
            scheduledTasks.synchronized {
              scheduledTasks.wait(sleepFor)
            }
          // retry execution
          waitOrExecute()
        }
    }
  }

  private[this] def getCurrentTime = System.currentTimeMillis()

  private[this] implicit object TaskOrdering extends Ordering[TaskKey] {
    def compare(x: TaskKey, y: TaskKey): Int =
      if (x.runsAt < y.runsAt)
        -1
      else if (x.runsAt == y.runsAt) {
        if (x.id < y.id)
          -1
        else if (x.id == y.id)
          0
        else
          1
      }
      else
        1
  }

  private[this] val scheduledTasks =
    Ref(TreeMap.empty[TaskKey, TaskCallback])

  private[this] val lastID = Ref(0L)

  locally {
    val th = new Thread(new Runnable {
      def run() {
        waitOrExecute()
      }
    })

    th.setDaemon(true)
    th.setName("shifter-scheduler")
    th.start()
  }
}
