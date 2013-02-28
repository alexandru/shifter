package shifter.db

import annotation.tailrec
import java.sql._
import collection.immutable.Queue
import java.util.concurrent.atomic.{AtomicReference, AtomicBoolean}


case class DBPool(
                   url: String,
                   user: Option[String],
                   password: Option[String],
                   driver: Option[String],
                   poolSize: Int)
  extends IDB {

  require(poolSize > 1)

  private[this] val queue: AtomicReference[Queue[Connection]] = {
    val conns = (0 until poolSize).map(x => getConnection())
    new AtomicReference(Queue.empty[Connection] ++ conns)
  }

  lazy val adapter =
    adapters.DBAdapter.adapterForUrl(url)

  def withSession[A](f: Connection => A): A = {
    val conn = acquireConnection()

    try {
      f(conn)
    } finally {
      releaseConnection(conn)
    }
  }

  def withTransaction[A](f: Connection => A): A = {
    val conn = acquireConnection()
    conn.setAutoCommit(false)

    try {
      f(conn)
    }
    catch {
      case ex: Throwable =>
        conn.rollback()
        throw ex
    }
    finally {
      conn.setAutoCommit(true)
      releaseConnection(conn)
    }
  }

  private[this] def getConnection() =
    adapter.initConnection(url, user, password, driver)

  private[this] def releaseConnection(conn: Connection) {
    var queueRef: Queue[Connection] = null
    var newQueue: Queue[Connection] = null

    val released = if (conn.isClosed)
      getConnection
    else
      conn

    do {
      queueRef = queue.get
      newQueue = queueRef :+ released
    } while (!queue.compareAndSet(queueRef, newQueue))

    queue.synchronized {
      queue.notify
    }
  }

  private[this] def acquireConnection(): Connection = {
    var returnConn: Connection = null

    // fetch connection from the queue 
    while (returnConn == null) {
      var queueRef: Queue[Connection] = queue.get

      while (queueRef == null || queueRef.isEmpty)
        queue.synchronized {
          queue.wait
          queueRef = queue.get
        }

      try {
        val (conn, newQueue) = queueRef.dequeue
        if (queue.compareAndSet(queueRef, newQueue))
          returnConn = conn
      } catch {
        case ex: NoSuchElementException =>
      }
    }

    returnConn
  }
}


object DBPool {
  def apply(url: String, poolSize: Int): DBPool =
    DBPool(url, None, None, None, poolSize)

  def apply(url: String, user: String, password: String, poolSize: Int): DBPool =
    DBPool(url, Option(user), Option(password), None, poolSize)

  def apply(url: String, driver: String, poolSize: Int): DBPool =
    DBPool(url, None, None, Option(driver), poolSize)

  def apply(url: String, user: String, password: String, driver: String, poolSize: Int): DBPool =
    DBPool(url, Option(user), Option(password), Option(driver), poolSize)
}
