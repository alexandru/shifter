package shifter.db.adapters

import shifter.lang.backports._
import java.lang.ClassNotFoundException
import java.sql.{DriverManager, Connection, SQLException}


class MySQLAdapter extends DBAdapter("mysql") {
  override def initConnection(url: String, user: String, password: String): Connection = 
    Try {
      DriverManager.getConnection(url, user, password)
    } 
    .recover {
      // attempt loading driver
      case ex: SQLException if ex.getSQLState == "08001" =>	
	Class.forName("com.mysql.jdbc.Driver")
	DriverManager.getConnection(url, user, password)
    }
    match {
      case Success(conn) => conn
      case Failure(ex) => throw ex
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
