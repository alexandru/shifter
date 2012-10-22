package shifter.db.adapters

import java.sql.{DriverManager, Connection}


class DerbyAdapter extends DBAdapter("derby") {
  override def initConnection(url: String, user: String, password: String): Connection = {
    if (url.startsWith("jdbc:derby://"))
	Class.forName("org.apache.derby.jdbc.ClientDriver")
    else
	Class.forName("org.apache.derby.jdbc.EmbeddedDriver")

    DriverManager.getConnection(url, user, password)
  }
}
