package shifter.db

import java.sql.Connection


object Conversions {
  implicit def `Connection -> DBConnection`(conn: Connection) =
    new DBConnection(conn)
}
