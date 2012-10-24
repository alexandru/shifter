package shifter.db.adapters

import shifter.lang.backports._
import java.lang.ClassNotFoundException
import java.sql.{DriverManager, Connection, SQLException}


class HSQLDBAdapter extends DBAdapter("hsqldb") {
  override def initConnection(url: String, user: String, password: String): Connection = 
    Try {
      DriverManager.getConnection(url, user, password)
    } 
    .recover {
      // no driver loaded, attempt loading driver
      case ex: SQLException if ex.getSQLState == "08001" =>        
        Class.forName("org.hsqldb.jdbcDriver")
    }
    .recover {
      // if it fails, attempt loading legacy driver
      case ex: ClassNotFoundException =>
        Class.forName("org.hsqldb.jdbc.JDBCDriver")
    }
    // return the connection or fail hard
    match {
      case Failure(ex) => 
        throw ex
      case Success(_: Class[_]) =>
        DriverManager.getConnection(url, user, password)
      case Success(conn: Connection) => 
        conn
    }
}
