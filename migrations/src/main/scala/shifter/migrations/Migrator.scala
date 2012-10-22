package shifter.migrations

import shifter.reflection._


abstract class Migrator(val packageName: String, val group: String) {
  def setup(): Unit
  def persistVersion(version: Int): Unit
  def currentVersion: Int

  def initMigration(cls: Class[Migration]) = 
    toInstance(cls)

  lazy val migrations: Seq[Migration] = {
    val classes = findSubTypes[Migration](packageName).filterNot(_.isInterface)
    val instances = classes.flatMap(c => initMigration(c))
    val setup = instances.filter(_.group == group)
    setup.toList.sortBy(_.version)
  } 

  lazy val latestMigration: Option[Migration] = {
    if (migrations.isEmpty)
      None
    else
      Some(migrations.last)
  }

  def currentMigration = 
    migrations.filter(_.version == currentVersion).headOption

  def nextMigration = 
    migrations.find(_.version > currentVersion)

  def prevMigration = 
    migrations.reverse.find(_.version < currentVersion)

  def migrationsFor(version: Int): Seq[Migration] = {
    val current = currentVersion

    if (current == version)
      Seq.empty[Migration]
    else if (current < version)
      migrations.filter(m => m.version > current && m.version <= version).sortBy(_.version)
    else
      migrations.filter(m => m.version <= current && m.version > version).sortBy(-_.version)
  }

  def migrateTo(version: Int): Boolean = {
    val current = currentVersion
    val migrations = migrationsFor(version)

    if (current < version) {
      migrations.foreach { m => 
	m.up()
	persistVersion(m.version)
      }
      true
    }
    else if (current > version) {
      migrations.foreach { m => 
	persistVersion(m.version)
	m.down()
      }

      persistVersion(version)
      true
    }
    else 
      false
  }

  def migrateOneDown(): Boolean = {
    val prev = prevMigration

    val ret = currentMigration match {
      case Some(current) => 
	current.down()
        true
      case None =>
	false
    }

    prev match {
      case Some(m) => 
	persistVersion(m.version)
      case None =>
	persistVersion(0)
    }    

    ret
  }

  def migrateOneUp(): Boolean =
    nextMigration match {
      case Some(m) => 
	m.up()
	persistVersion(m.version)
	true
      case None =>
	false
    }

  def migrate(): Boolean =
    if (migrateOneUp()) {
      migrate()
      true
    }
    else
      false

  def migrateToZero(): Boolean =
    if (migrateOneDown()) {
      migrateToZero()
      true
    }
    else
      false
}
