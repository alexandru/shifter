package shifter.db

import java.sql.{DriverManager, Connection}


class DerbyAdapter extends DBAdapter("derby") {
  override def initConnection(url: String, user: String, password: String): Connection = {
    if (url.startsWith("jdbc:derby://"))
	Class.forName("org.apache.derby.jdbc.ClientDriver")
    else
	Class.forName("org.apache.derby.jdbc.EmbeddedDriver")

    DriverManager.getConnection(url, user, password)
  }

  override def listTables(conn: Connection): Seq[String] = {
    val rs = conn.getMetaData.getTables(null, null, "%", null)
    var tables = Seq.empty[String]
    
    while (rs.next) 
      tables = tables :+ rs.getString(3) 

    tables
  }
}
