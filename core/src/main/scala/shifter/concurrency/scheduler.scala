package shifter.concurrency

import collection.immutable.TreeMap
import annotation.tailrec
import scala.concurrent.{Promise, Future, ExecutionContext}
import util.control.NonFatal
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.atomic.Atomic


/**
 * Light-weight task scheduler.
 *
 * It is used for Futures/Promises timeouts, being useful instead of working with
 * [[http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Executors.html#newScheduledThreadPool() Executors.newScheduledThreadPool()]]
 * in cases you need to cancel the scheduling of a task, freeing up the resources such that
 * garbage collecting can take place.
 *
 * To schedule a task to execute: {{{
 *
 *   // we need an ExecutionContext for executing the given computation
 *   import concurrent.ExecutionContext.Implicits.global
 *   // helpers for specifying the delay in milliseconds
 *   import concurrent.duration._
 *
 *   val task = scheduler.runOnce(10.seconds.toMillis) {
 *     println("Hello, after 10 seconds")
 *   }
 *
 *   // ...
 *   // in case you later want to cancel the scheduled task
 *   scheduler.cancel(task)
 *
 * }}}
 *
 * You can also schedule a
 * [[http://www.scala-lang.org/api/current/index.html#scala.concurrent.Future Future]]
 * for execution after a given delay: {{{
 *
 *   // we need an ExecutionContext for executing the given computation
 *   import concurrent.ExecutionContext.Implicits.global
 *   // for specifying the delay
 *   import concurrent.duration._
 *
 *   val future: Future[String] = scheduler.future(10.seconds) {
 *     "Hello, after 10 seconds"
 *   }
 *
 *   val result = Await.result(future, 10.seconds)
 *
 * }}}
 */
object scheduler {

  /**
   * Is returned by `runOnce` as a task identifier and can
   * be used to cancel a scheduled task.
   *
   * @param id - unique per-process ID of the scheduled task
   * @param runsAt - Unix timestamp in milliseconds of when the task is scheduled to run
   */
  case class TaskKey(id: Long, runsAt: Long)

  /**
   * Internal value used for storing the callback
   * to be executed after the given delay and its related
   * execution context
   */
  private[this] case class TaskCallback(
    run: () => Any,
    ec: ExecutionContext
  )

  /**
   * Schedule a
   * [[http://www.scala-lang.org/api/current/index.html#scala.concurrent.Future Future]]
   * for completion after the specified delay.
   *
   * @param initialDelay - how much to wait before this future is completed
   * @param cb - the function to execute
   * @return - a future
   *
   * @example {{{
   *
   *   val future: Future[String] = scheduler.future(10.seconds) {
   *     "Value that will be available after 10 seconds"
   *   }
   *
   * }}}
   */
  def future[T](initialDelay: FiniteDuration)(cb: => T)(implicit ec: ExecutionContext): Future[T] = {
    val promise = Promise[T]()
    runOnce(initialDelay.toMillis) {
      promise.completeWith(Future(cb))
    }
    promise.future
  }

  /**
   * Schedules a function for execution after a specified delay.
   *
   * @param delayMillis - the initial delay to wait before executing the function
   * @param callback - the function to execute
   * @param ec - the execution context used for executing the function
   * @return - a `TaskKey` that can be used for canceling the scheduling of this task
   */
  def runOnce(delayMillis: Long)(callback: => Any)(implicit ec: ExecutionContext): TaskKey = {
    val newID = lastID.incrementAndGet
    val task = TaskKey(newID, System.currentTimeMillis() + delayMillis)
    pushTask(task, TaskCallback(() => callback, ec))
    scheduledTasks.synchronized { scheduledTasks.notifyAll() }
    task
  }

  /**
   * Cancels a pending task
   *
   * @param task - the `TaskKey` used as an identifier for the task to cancel
   * @return - either `true` in case a task was canceled, or `false` otherwise
   */
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

  /**
   * Pushes a new task in the queue of tasks to be executed.
   */
  @tailrec
  private[this] def pushTask(task: TaskKey, callback: TaskCallback) {
    val tasks = scheduledTasks.get
    if (!tasks.contains(task))
      if (!scheduledTasks.compareAndSet(tasks, tasks.updated(task, callback)))
        pushTask(task, callback)
  }

  /**
   * Run-loop for executing scheduled tasks.
   *
   * In case we've got no tasks to run, the sleep and try again later.
   * In case we've got tasks to run in the future, sleep for the exact
   * amount of time until the next task in line is due. In case we've got
   * a task that needs executing, then do so.
   */
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

  /**
   * The queue is in fact a priority queue modeled by means of a simple
   * `OrderedMap`, in which the keys are `TaskKey`, so we need an implementation
   * for `Ordering[TaskKey]`
   */
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

  /**
   * Our internal priority queue for scheduled tasks.
   */
  private[this] val scheduledTasks =
    Atomic(TreeMap.empty[TaskKey, TaskCallback])

  /**
   * For generating unique IDs of scheduled tasks.
   */
  private[this] val lastID = Atomic(0L)

  /**
   * Start thread that does it all.
   */
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
