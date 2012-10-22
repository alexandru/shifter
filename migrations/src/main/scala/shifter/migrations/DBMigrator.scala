package shifter.migrations

import java.sql.Connection
import shifter.lang._
import shifter.db._
import shifter.db.Conversions._


abstract class DBMigrator(packageName: String, group: String, val conn: Connection)
extends Migrator(packageName, group) {
  override def setup() {
    if (!conn.toDB.listTables.exists(_.toLowerCase == "shiftermigrations")) {
      using (conn.createStatement)(
	_.execute("CREATE TABLE shiftermigrations (mname VARCHAR(100) NOT NULL, mvalue VARCHAR(100) DEFAULT NULL)"))
      using (conn.createStatement)(
	_.execute("ALTER TABLE shiftermigrations ADD CONSTRAINT shiftermigrationsname UNIQUE (mname)"))

      using (conn.prepareStatement("INSERT INTO shiftermigrations (mname, mvalue) VALUES (?, ?)")) {
	stm =>
	  stm.setString(1, "version")
	  stm.setString(2, "0")
	  stm.executeUpdate
      }
    }
  }

  override def persistVersion(version: Int) {
    val sql = "UPDATE shiftermigrations SET mvalue = ? WHERE mname = ?"

    using (conn.prepareStatement(sql)) { 
      stm =>
	stm.setString(1, version.toString)
	stm.setString(2, "version")
	stm.executeUpdate
    }
  }

  override def currentVersion = {
    val sql = "SELECT mvalue FROM shiftermigrations WHERE mname = ?"

    using (conn.prepareStatement(sql)) { 
      stm =>
	stm.setString(1, "version")
	val rs = stm.executeQuery
	if (rs.next)
	  rs.getString(1).toInt
	else
	  0
    }    
  }
}
