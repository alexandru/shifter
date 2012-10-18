package shifter.db

import java.sql.Connection


object Conversions {
  class DBConnectionHelper(conn: Connection) {
    def toDB = new DB(conn)
  }

  implicit def `Connection -> DBConnectionHelper`(conn: Connection) =
    new DBConnectionHelper(conn)
}
