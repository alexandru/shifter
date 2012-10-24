package shifter.migrations

import shifter.reflection._


final class MigratorSession(migrator: Migrator) {
  def currentVersion = 
    migrator.currentVersion

  def migrateTo(version: Int): Boolean = {
    val current = currentVersion
    val migrations = migrationsFor(version)

    if (current < version) {
      migrations.foreach { m => 
	goUp(m)
	persistVersion(m.version)
      }
      true
    }
    else if (current > version) {
      migrations.foreach { m => 
	persistVersion(m.version)
	goDown(m)
      }

      persistVersion(this.migrations.reverse.find(_.version < currentVersion) match {
	case Some(v) => v.version
	case None => 0
      })

      true
    }
    else 
      false
  }

  def migrateOneDown(): Boolean = {
    val prev = prevMigration

    val ret = currentMigration match {
      case Some(current) => 
	goDown(current)
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

  def migrateOneUp(): Boolean = {
    nextMigration match {
      case Some(m) => 
	goUp(m)
	persistVersion(m.version)
	true
      case None =>
	false
    }
  }

  def migrate(): Boolean = {
    @annotation.tailrec
    def iter(ret: Boolean): Boolean = 
      if (migrateOneUp())
	iter(true)
      else
	ret
    iter(false)
  }

  def migrateToZero(): Boolean = {
    @annotation.tailrec
    def iter(ret: Boolean): Boolean = 
      if (migrateOneDown())
	iter(true)
      else
	ret
    iter(false)
  }

  protected def withSession[A](f: => A): A = {
    setup()
    if (currentVersion > 0)
      logger.info("Migration system initiated (starting from version "+currentVersion+")")
    else
      logger.info("Migration system initiated (starting from zero)")

    val ret = f
    logger.info("  STOPPED (version " + currentVersion + ")")
    ret
  }

  protected def currentMigration = 
    migrations.filter(_.version == currentVersion).headOption

  protected def nextMigration = 
    migrations.find(_.version > currentVersion)

  protected def prevMigration = 
    migrations.reverse.find(_.version < currentVersion)

  protected def migrationsFor(version: Int): Seq[Migration] = {
    val current = currentVersion

    if (current == version)
      Seq.empty[Migration]
    else if (current < version)
      migrations.filter(m => m.version > current && m.version <= version).sortBy(_.version)
    else
      migrations.filter(m => m.version <= current && m.version > version).sortBy(-_.version)
  }

  protected def goUp(m: Migration) = {
    m.up()
    logger.info("  UP on version " + m.version)
  }

  protected def goDown(m: Migration) = {
    m.down()
    logger.info("  DOWN on version " + m.version)
  }

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

  val group = migrator.group

  private[this] lazy val logger = migrator.logger

  private[this] val packageName = migrator.packageName

  private[this] def setup() = migrator.setup()

  private[this] def persistVersion(v: Int) = migrator.persistVersion(v)

  private[this] def initMigration(cls: Class[Migration]) = 
    migrator.initMigration(cls)
}
