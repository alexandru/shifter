package shifter.migrations

import java.sql.Connection
import shifter.reflection.toInstance


class DBTestMigrator(implicit conn: Connection) 
extends DBMigrator("shifter.migrations", "Test DB", conn) {

  override def initMigration(cls: Class[Migration]) = 
    toInstance(cls, conn)
    
}

abstract class DBTestMigration(version: Int) 
extends Migration("Test DB", version)


class DBTestM001(conn: Connection) extends DBTestMigration(1) {
  def up() {
    val sql = """CREATE TABLE test1 (id INT PRIMARY KEY, name1 VARCHAR(100))"""
    val stm = conn.prepareStatement(sql)
    stm.execute
    stm.close
  }

  def down() {
    val sql = """DROP TABLE test1"""
    val stm = conn.prepareStatement(sql)
    stm.execute
    stm.close
  }
}


class DBTestM002(conn: Connection) extends DBTestMigration(2) {
  def up() {
    val sql = """CREATE TABLE test2 (id INT PRIMARY KEY, name2 VARCHAR(100))"""
    val stm = conn.prepareStatement(sql)
    stm.execute
    stm.close
  }

  def down() {
    val sql = """DROP TABLE test2"""
    val stm = conn.prepareStatement(sql)
    stm.execute
    stm.close
  }
}

class DBTestM003(conn: Connection) extends DBTestMigration(3) {
  def up() {
    val sql = """CREATE TABLE test3 (id INT PRIMARY KEY, name3 VARCHAR(100))"""
    val stm = conn.prepareStatement(sql)
    stm.execute
    stm.close
  }

  def down() {
    val sql = """DROP TABLE test3"""
    val stm = conn.prepareStatement(sql)
    stm.execute
    stm.close
  }
}
