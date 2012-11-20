package shifter.db.adapters

import java.sql.Connection


class MySQLAdapter extends DBAdapter("mysql") {
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
