package shifter.db

import java.sql.Connection
import java.sql.SQLException
import shifter.reflection._
import shifter.lang._
import shifter.db.adapters._


class DBException(msg: String) extends SQLException(msg)

case class DB(url: String, user: Option[String], password: Option[String], driver: Option[String]) {
  def withSession[A](f: Connection => A): A = {
    val underlying = adapter.initConnection(url, user, password, driver)
    try {
      f(underlying)
    }
    finally {
      underlying.close()
    }
  }

  def withTransaction[A](f: Connection => A): A = 
    withSession {
      underlying =>

      underlying.setAutoCommit(false)

      try {
	val ret = f(underlying)
	underlying.commit()
	ret
      } 
      catch {
	case ex: Throwable =>
	  underlying.rollback()
	  throw ex
      }
      finally {
	underlying.setAutoCommit(true)
      }
    }

  def listTables(implicit conn: Connection): Seq[String] = 
    adapter.listTables(conn)

  private[this] val adapter = 
    DBAdapter.adapterForUrl(url)
}


object DB {
  def apply(url: String): DB =
    DB(url, None, None, None)

  def apply(url: String, user: String, password: String): DB =
    DB(url, Option(user), Option(password), None)

  def apply(url: String, driver: String): DB =
    DB(url, None, None, Option(driver))

  def apply(url: String, user: String, password: String, driver: String): DB = 
    DB(url, Option(user), Option(password), Option(driver))  
}
