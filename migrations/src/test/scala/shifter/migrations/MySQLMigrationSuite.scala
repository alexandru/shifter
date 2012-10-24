package shifter.migrations

import shifter.db._
import shifter.db.Conversions._
import shifter.lang.using
import java.sql.Connection
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException
import org.scalatest.exceptions.TestPendingException


@RunWith(classOf[JUnitRunner])
class MySQLMigrationSuite extends FunSuite {
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
	assert(m.currentVersion === 1)
	val tables = conn.toDB.listTables
	assert(tables.size === 2)
	assert(tables.contains("shiftermigrations"))
	assert(tables.contains("test1"))
    }
  }

  test("Migrate to version 2, migrateOneUp()") {
    withMigrator {
      (conn, m) => 
	assert(m.migrateOneUp)
	assert(m.migrateOneUp)
	assert(m.currentVersion === 2)
	val tables = conn.toDB.listTables
	assert(tables.size === 3)
	assert(tables.contains("shiftermigrations"))
	assert(tables.contains("test1"))
	assert(tables.contains("test2"))
    }
  }

  test("Migrate to version 2, migrateTo()") {
    withMigrator {
      (conn, m) => 
	assert(m.migrateTo(2))
	assert(m.currentVersion === 2)
	val tables = conn.toDB.listTables
	assert(tables.size === 3)
	assert(tables.contains("shiftermigrations"))
	assert(tables.contains("test1"))
	assert(tables.contains("test2"))
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
	assert(tables.size === 4)
	assert(tables.contains("shiftermigrations"))
	assert(tables.contains("test1"))
	assert(tables.contains("test2"))
	assert(tables.contains("test3"))
    }
  }

  test("Migrate to version 3, migrateTo()") {
    withMigrator {
      (conn, m) => 
	assert(m.migrateTo(3))
	assert(m.currentVersion === 3)
	val tables = conn.toDB.listTables
	assert(tables.size === 4)
	assert(tables.contains("shiftermigrations"))
	assert(tables.contains("test1"))
	assert(tables.contains("test2"))
	assert(tables.contains("test3"))
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
	assert(tables.size === 3)
	assert(tables.contains("shiftermigrations"))
	assert(tables.contains("test1"))
	assert(tables.contains("test2"))
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
	assert(tables.size === 2)
	assert(tables.contains("shiftermigrations"))
	assert(tables.contains("test1"))
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
	assert(tables.size === 4)
	assert(tables.contains("shiftermigrations"))
	assert(tables.contains("test1"))
	assert(tables.contains("test2"))
	assert(tables.contains("test3"))
    }
  }

  test("Migrate down to zero and up again") {
    withMigrator {
      (conn, m) => 
	assert(m.migrateTo(3))
	assert(m.currentVersion === 3)

	assert(m.migrateToZero)
	assert(m.currentVersion === 0)
	val tablesVer0 = conn.toDB.listTables
	assert(tablesVer0.size === 1)
	assert(tablesVer0.contains("shiftermigrations"))

	assert(m.migrate)
	assert(m.currentVersion === 3)

	val tables = conn.toDB.listTables
	assert(tables.size === 4)
	assert(tables.contains("shiftermigrations"))
	assert(tables.contains("test1"))
	assert(tables.contains("test2"))
	assert(tables.contains("test3"))
    }
  }

  def withMigrator(f: (Connection, MigratorSession) => Any) {
    var conn: Connection = null 
    try {
      conn = DB("jdbc:mysql://localhost:3306/shifter", "root", "").underlying
    } catch {
      case ex: MySQLSyntaxErrorException =>
	throw new TestPendingException()
    }

    using (conn) {
      inst =>
	implicit val db = inst

      // resets everything
      db.toDB.listTables.foreach {
	tblName =>
	  val stm = db.prepareStatement("DROP TABLE " + tblName)
	stm.execute
	stm.close
      }

      new DBTestMigrator().withSession { s => f(db, s) }
    }
  }
}
