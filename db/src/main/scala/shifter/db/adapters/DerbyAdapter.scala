package shifter.db.adapters

import shifter.lang._
import shifter.lang.backports._
import java.lang.ClassNotFoundException
import java.sql.{DriverManager, Connection, SQLException}


class DerbyAdapter extends DBAdapter("derby") {
  override def initConnection(url: String, user: String, password: String): Connection = 
    Try {
      DriverManager.getConnection(url, user, password)
    } 
    .recover {
      case ex: SQLException if ex.getSQLState == "08001" =>
	if (url.startsWith("jdbc:derby://"))
	  Class.forName("org.apache.derby.jdbc.ClientDriver")
	else
	  Class.forName("org.apache.derby.jdbc.EmbeddedDriver")
	DriverManager.getConnection(url, user, password)
    }
    match {
      case Success(conn) => conn
      case Failure(ex) => throw ex
    }
}
