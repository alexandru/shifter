package shifter.migrations

import shifter.db.Conversions._
import shifter.db.DBConnection
import shifter.reflection.using
import java.sql.{Connection, DriverManager}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.apache.commons.io.FileUtils
import java.io.File


@RunWith(classOf[JUnitRunner])
class DerbyMigrationSuite extends FunSuite {
  test("First version should be zero") {
    withMigrator {
      (conn, m) => 
	assert(m.currentVersion === 0)
    }
  }

  test("Migrate to version 1") {
    withMigrator {
      (conn, m) => 
	assert(m.migrateOneUp)

	val dbVersion = conn.fetchOne("SELECT mvalue FROM shiftermigrations WHERE mname = ?", "version").getOrElse("0").toInt
	assert(dbVersion === 1)
	assert(m.currentVersion === 1)

	val tables = conn.listTables
	assert(tables.contains("SHIFTERMIGRATIONS"))
	assert(tables.contains("TEST1"))
	assert(!tables.contains("TEST2"))
	assert(!tables.contains("TEST3"))
    }
  }

  test("Migrate to version 2, migrateOneUp()") {
    withMigrator {
      (conn, m) => 
	assert(m.migrateOneUp)
	assert(m.migrateOneUp)
	assert(m.currentVersion === 2)
	val tables = conn.listTables
	assert(tables.contains("SHIFTERMIGRATIONS"))
	assert(tables.contains("TEST1"))
	assert(tables.contains("TEST2"))
	assert(!tables.contains("TEST3"))
    }
  }

  test("Migrate to version 2, migrateTo()") {
    withMigrator {
      (conn, m) => 
	assert(m.migrateTo(2))
	assert(m.currentVersion === 2)
	val tables = conn.listTables
	assert(tables.contains("SHIFTERMIGRATIONS"))
	assert(tables.contains("TEST1"))
	assert(tables.contains("TEST2"))
	assert(!tables.contains("TEST3"))
    }
  }

  test("Migrate to version 3, migrateOneUp()") {
    withMigrator {
      (conn, m) => 
	assert(m.migrateOneUp)
	assert(m.migrateOneUp)
	assert(m.migrateOneUp)
	assert(m.currentVersion === 3)
	val tables = conn.listTables
	assert(tables.contains("SHIFTERMIGRATIONS"))
	assert(tables.contains("TEST1"))
	assert(tables.contains("TEST2"))
	assert(tables.contains("TEST3"))
    }
  }

  test("Migrate to version 3, migrateTo()") {
    withMigrator {
      (conn, m) => 
	assert(m.migrateTo(3))
	assert(m.currentVersion === 3)
	val tables = conn.listTables
	assert(tables.contains("SHIFTERMIGRATIONS"))
	assert(tables.contains("TEST1"))
	assert(tables.contains("TEST2"))
	assert(tables.contains("TEST3"))
    }
  }

  test("Migrate down to version 2, migrateOneDown()") {
    withMigrator {
      (conn, m) => 
	assert(m.migrateTo(3))
	assert(m.currentVersion === 3)
	assert(m.migrateOneDown)
	assert(m.currentVersion === 2)
	val tables = conn.listTables
	assert(tables.contains("SHIFTERMIGRATIONS"))
	assert(tables.contains("TEST1"))
	assert(tables.contains("TEST2"))
        assert(!tables.contains("TEST3"))
    }
  }

  test("Migrate down to version 1, migrateTo()") {
    withMigrator {
      (conn, m) => 
	assert(m.migrateTo(3))
	assert(m.currentVersion === 3)
	assert(m.migrateTo(1))
	assert(m.currentVersion === 1)
	val tables = conn.listTables
	assert(tables.contains("SHIFTERMIGRATIONS"))
	assert(tables.contains("TEST1"))
	assert(!tables.contains("TEST2"))
        assert(!tables.contains("TEST3"))
    }
  }

  test("Migrate down to version 1 and up again") {
    withMigrator {
      (conn, m) => 
	assert(m.migrateTo(3))
	assert(m.currentVersion === 3)
	assert(m.migrateTo(1))
	assert(m.currentVersion === 1)
	assert(m.migrate)
	assert(m.currentVersion === 3)
	val tables = conn.listTables
	assert(tables.contains("SHIFTERMIGRATIONS"))
	assert(tables.contains("TEST1"))
	assert(tables.contains("TEST2"))
	assert(tables.contains("TEST3"))
    }
  }

  test("Migrate down to zero and up again and down again") {
    withMigrator {
      (conn, m) => 
	assert(m.migrateTo(3))
	assert(m.currentVersion === 3)

	assert(m.migrateToZero)
	assert(m.currentVersion === 0)
	val tablesVer0 = conn.listTables
	assert(tablesVer0.contains("SHIFTERMIGRATIONS"))
	assert(!tablesVer0.contains("TEST1"))
	assert(!tablesVer0.contains("TEST2"))
	assert(!tablesVer0.contains("TEST3"))

	assert(m.migrate)
	assert(m.currentVersion === 3)

	val tables = conn.listTables
	assert(tables.contains("SHIFTERMIGRATIONS"))
	assert(tables.contains("TEST1"))
	assert(tables.contains("TEST2"))
	assert(tables.contains("TEST3"))

	assert(m.migrateToZero)
	assert(m.currentVersion === 0)

	val tablesFinal = conn.listTables
	assert(tablesFinal.contains("SHIFTERMIGRATIONS"))
	assert(!tablesFinal.contains("TEST1"))
	assert(!tablesFinal.contains("TEST2"))
	assert(!tablesFinal.contains("TEST3"))
    }
  }

  def withMigrator(f: (Connection, Migrator) => Any) {
    using (DBConnection("jdbc:derby:memory:shiftertest;create=true", "SA", "")) {
      inst =>
	implicit val db = inst

      val migrator = new DBTestMigrator
      migrator.setup()
      
      f(db, migrator)

      db.changeSchema("DROP TABLE TEST3", true)
      db.changeSchema("DROP TABLE TEST2", true)
      db.changeSchema("DROP TABLE TEST1", true)
      db.changeSchema("DROP TABLE SHIFTERMIGRATIONS")
    }
  }
}
