package shifter.db

import java.sql.Connection
import java.sql.SQLException
import shifter.db.adapters._

class DBException(msg: String) extends SQLException(msg)

trait IDB {
  def withConnection[A](f: Connection => A): A

  def withTransaction[A](f: Connection => A): A

  def adapter: DBAdapter

  def listTables(implicit conn: Connection): Seq[String] =
    adapter.listTables(conn)
}

case class DB(url: String, user: Option[String], password: Option[String], driver: Option[String]) extends IDB {
  def withConnection[A](f: Connection => A): A = {
    val underlying = adapter.initConnection(url, user, password, driver)
    try {
      f(underlying)
    }
    finally {
      underlying.close()
    }
  }

  def withTransaction[A](f: Connection => A): A =
    withConnection {
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

  lazy val adapter =
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
