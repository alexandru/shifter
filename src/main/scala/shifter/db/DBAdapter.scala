package shifter.db

import java.sql.Connection


abstract class DBAdapter(val dbIdentifier: String) {
  def initConnection(url: String, user: String, password: String): Connection

  def listTables(conn: Connection): Seq[String]
}


