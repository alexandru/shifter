package shifter.db.adapters

import scala.util._
import java.sql._
import java.lang.Class


class MySQLAdapter extends DBAdapter("mysql") {
  override
  def initConnection(url: String, user: Option[String], password: Option[String], driver: Option[String]): Connection = {
    Try {
      getConn(url, user, password)
    }
      .recover {
      // attempt loading driver
      case ex: SQLException if ex.getSQLState == "08001" =>
        Class.forName("com.mysql.jdbc.Driver")
        super.initConnection(url, user, password, driver)
    }
    match {
      case Failure(ex) =>
        throw ex
      case Success(conn: Connection) =>
        conn
    }
  }

  override def listTables(conn: Connection): Seq[String] = {
    val sql = "SHOW TABLES"

    val stm = conn.createStatement()
    val rs = stm.executeQuery(sql)
    var tables = Seq.empty[String]

    while (rs.next)
      tables = tables :+ rs.getString(1)

    tables
  }
}
