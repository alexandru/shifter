package shifter.db

import java.sql.{DriverManager, Connection}


class HSQLDBAdapter extends DBAdapter("hsqldb") {
  override def initConnection(url: String, user: String, password: String): Connection = {
    tryLoadDrivers(
      "org.hsqldb.jdbc.JDBCDriver",
      "org.hsqldb.jdbcDriver"
    )

    DriverManager.getConnection(url, user, password)    
  }

  override def listTables(conn: Connection): Seq[String] = {
    val rs = conn.getMetaData.getTables(null, null, "%", null)
    var tables = Seq.empty[String]
    
    while (rs.next) 
      tables = tables :+ rs.getString(3) 

    tables
  }

  private[this] def tryLoadDrivers(drivers: String*) {
    for (name <- drivers) 
      try {
	Class.forName(name)
	return
      } catch {
	case ex: ClassNotFoundException => 
      }
  }
}
