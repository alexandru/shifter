package shifter.db.adapters

import java.sql.{DriverManager, Connection}


class HSQLDBAdapter extends DBAdapter("hsqldb") {
  override def initConnection(url: String, user: String, password: String): Connection = {
    tryLoadDrivers(
      "org.hsqldb.jdbc.JDBCDriver",
      "org.hsqldb.jdbcDriver"
    )

    DriverManager.getConnection(url, user, password)    
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
