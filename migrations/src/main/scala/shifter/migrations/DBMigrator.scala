package shifter.migrations

import shifter.db._
import shifter.lang._
import language.reflectiveCalls

abstract class DBMigrator(packageName: String, group: String, val db: DB)
  extends Migrator(packageName, group) {

  override def setup() {
    db.withConnection {
      implicit conn =>
        if (!db.listTables.exists(_.toLowerCase == "shiftermigrations")) {
          using (conn.createStatement()) {
            _.execute("CREATE TABLE shiftermigrations (mname VARCHAR(100) NOT NULL, mvalue VARCHAR(100) DEFAULT NULL)")
          }

          using(conn.createStatement)(
            _.execute("ALTER TABLE shiftermigrations ADD CONSTRAINT shiftermigrationsname UNIQUE (mname)"))

          using(conn.prepareStatement("INSERT INTO shiftermigrations (mname, mvalue) VALUES (?, ?)")) {
            stm =>
              stm.setString(1, "version")
              stm.setString(2, "0")
              stm.executeUpdate
          }
        }
    }
  }

  override def persistVersion(version: Int) {
    db.withConnection {
      conn =>
        val sql = "UPDATE shiftermigrations SET mvalue = ? WHERE mname = ?"
        using(conn.prepareStatement(sql)) {
          stm =>
            stm.setString(1, version.toString)
            stm.setString(2, "version")
            stm.executeUpdate
        }
    }
  }

  override def currentVersion = {
    db.withConnection {
      conn =>
        val sql = "SELECT mvalue FROM shiftermigrations WHERE mname = ?"
        using(conn.prepareStatement(sql)) {
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
}
