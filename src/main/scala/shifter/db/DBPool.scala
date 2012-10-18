package shifter.db

import annotation.tailrec
import java.sql._
import collection.immutable.Queue
import java.util.concurrent.atomic.{AtomicReference, AtomicBoolean}


class DBPool private (url: String, user: String, password: String, poolSize: Int = 20) {  
  private[this] val queue: AtomicReference[Queue[DB]] = {
    val conns = (0 until poolSize).map(x => getConnection())
    new AtomicReference(Queue.empty[DB] ++ conns)
  }

  def withConnection[A](f: Connection => A): A = {
    val conn = acquireConnection()
    
    try {
      f(conn.underlying)
    } finally {
      releaseConnection(conn)
    }
  }

  def closeAll() {
    queue.synchronized {
      queue.get.foreach(conn => try {
	conn.underlying.close()} catch {case _ =>})
    }
  }

  @tailrec
  final def releaseConnection(conn: DB) {
    val queueRef = queue.get
    val newQueue = queueRef :+ conn
    if (!queue.compareAndSet(queueRef, newQueue))
      releaseConnection(conn)	
  }

  final def acquireConnection(): DB = {
    var returnConn: DB = null

    // fetch connection from the queue 
    while (returnConn == null) {
      var queueRef: Queue[DB] = queue.get

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

  private[this] def getConnection() =
    DB(url, user, password)
}


object DBPool {
  @volatile
  private[this] var map = Map.empty[String, DBPool]

  def apply(uid: String): DBPool = 
    apply(uid, null, null, null, 20)

  def apply(uid: String, url: String, user: String, password: String, initPoolSize: Int = 20): DBPool = {
    require(initPoolSize > 0, "Pool size must be a non-negative, positive number")

    if (map.contains(uid))
      map(uid)
    else {
      assert(
	url != null && user != null && password != null, 
	"Nonexistent pool with uid '"+uid+"', cannot create one without an URL and proper user credentials")
      createPool(uid, url, user, password, initPoolSize)
    }
  }

  private[this] def createPool(uid: String, url: String, user: String, password: String, poolSize: Int): DBPool = 
    this.synchronized {
      map.get(uid) match {
	case Some(pool) => pool
	case None => {
	  val pool = new DBPool(url, user, password, poolSize)
	  map = map + (uid -> pool)
	  pool
	}
      }
    }
}
