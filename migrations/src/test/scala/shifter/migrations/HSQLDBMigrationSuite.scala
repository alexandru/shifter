package shifter.migrations

import shifter.db._
import shifter.db.Conversions._
import shifter.lang._
import java.sql.{Connection, DriverManager}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.apache.commons.io.FileUtils
import java.io.File


@RunWith(classOf[JUnitRunner])
class HSQLDBMigrationSuite extends FunSuite {
  test("First version should be zero") {
    withMigrator {
      (conn, m) => 
	assert(m.currentVersion === 0)
    }
  }

  test("Migrate to version 1") {
    withMigrator {
      (conn, m) => 
	implicit val db = conn

	assert(m.migrateOneUp)

	val dbVersion = SQL("SELECT mvalue FROM shiftermigrations WHERE mname = ?").withArgs("version").
	  select.map(_[Int]("mvalue")).toStream.headOption.getOrElse(0)

	assert(dbVersion === 1)
	assert(m.currentVersion === 1)

	val tables = conn.toDB.listTables
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
	val tables = conn.toDB.listTables
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
	val tables = conn.toDB.listTables
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
	val tables = conn.toDB.listTables
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
	val tables = conn.toDB.listTables
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
	val tables = conn.toDB.listTables
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
	val tables = conn.toDB.listTables
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
	val tables = conn.toDB.listTables
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
	val tablesVer0 = conn.toDB.listTables
	assert(tablesVer0.contains("SHIFTERMIGRATIONS"))
	assert(!tablesVer0.contains("TEST1"))
	assert(!tablesVer0.contains("TEST2"))
	assert(!tablesVer0.contains("TEST3"))

	assert(m.migrate)
	assert(m.currentVersion === 3)

	val tables = conn.toDB.listTables
	assert(tables.contains("SHIFTERMIGRATIONS"))
	assert(tables.contains("TEST1"))
	assert(tables.contains("TEST2"))
	assert(tables.contains("TEST3"))

	assert(m.migrateToZero)
	assert(m.currentVersion === 0)

	val tablesFinal = conn.toDB.listTables
	assert(tablesFinal.contains("SHIFTERMIGRATIONS"))
	assert(!tablesFinal.contains("TEST1"))
	assert(!tablesFinal.contains("TEST2"))
	assert(!tablesFinal.contains("TEST3"))
    }
  }

  def withMigrator(f: (Connection, MigratorSession) => Any) {
    using (DB("jdbc:hsqldb:mem:shiftertest", "SA", "").underlying) {
      inst =>
	implicit val db = inst

      try {
	val migrator = new DBTestMigrator
	migrator.withSession(s => f(db, s))
      } finally {
	val stm = db.createStatement
	stm.execute("shutdown")
	stm.close
      }
    }
  }
}
