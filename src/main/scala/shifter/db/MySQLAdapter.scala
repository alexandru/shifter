package shifter.db

import java.sql.{DriverManager, Connection}

class MySQLAdapter extends DBAdapter("mysql") {
  override def initConnection(url: String, user: String, password: String): Connection = {
    Class.forName("com.mysql.jdbc.Driver")
    DriverManager.getConnection(url, user, password)
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
