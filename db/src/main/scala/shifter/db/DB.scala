package shifter.db

import java.sql.Connection
import java.sql.SQLException
import shifter.reflection._
import shifter.lang._
import shifter.db.adapters._


class DBException(msg: String) extends SQLException(msg)


class DB(val underlying: Connection) {
  def withSession[A](f: Connection => A): A =
    f(underlying)

  def withTransaction[A](f: Connection => A): A = {
    try {
      underlying.setAutoCommit(false)
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

  def listTables: Seq[String] =
    DBAdapter.adapterForConnection(underlying).listTables(underlying)
}


object DB {
  def apply(url: String, user: String, password: String) =     
    new DB(DBAdapter(url, user, password))
}
