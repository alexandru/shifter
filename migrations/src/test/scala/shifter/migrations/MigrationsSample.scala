package shifter.migrations

import shifter.db.DB
import shifter.reflection.toInstance


class DBTestMigrator(db: DB) 
extends DBMigrator("shifter.migrations", "Test DB", db) {

  override def initMigration(cls: Class[Migration]) = 
    toInstance(cls, db)
    
}

abstract class DBTestMigration(version: Int) 
extends Migration("Test DB", version)


class DBTestM001(db: DB) extends DBTestMigration(1) {
  def up() {
    db.withSession { 
      conn =>
        val sql = """CREATE TABLE test1 (id INT PRIMARY KEY, name1 VARCHAR(100))"""
	val stm = conn.prepareStatement(sql)
	stm.execute
	stm.close
    }
  }

  def down() {
    db.withSession { 
      conn =>
	val sql = """DROP TABLE test1"""
	val stm = conn.prepareStatement(sql)
	stm.execute
	stm.close
    }
  }
}


class DBTestM002(db: DB) extends DBTestMigration(2) {
  def up() {
    db.withSession { 
      conn =>
	val sql = """CREATE TABLE test2 (id INT PRIMARY KEY, name2 VARCHAR(100))"""
	val stm = conn.prepareStatement(sql)
	stm.execute
	stm.close
    }
  }

  def down() {
    db.withSession { 
      conn =>
	val sql = """DROP TABLE test2"""
	val stm = conn.prepareStatement(sql)
	stm.execute
	stm.close
    }
  }
}

class DBTestM003(db: DB) extends DBTestMigration(3) {
  def up() {
    db.withSession { 
      conn =>
	val sql = """CREATE TABLE test3 (id INT PRIMARY KEY, name3 VARCHAR(100))"""
	val stm = conn.prepareStatement(sql)
	stm.execute
	stm.close
    }
  }

  def down() {
    db.withSession { 
      conn =>
	val sql = """DROP TABLE test3"""
	val stm = conn.prepareStatement(sql)
	stm.execute
	stm.close
    }
  }
}
