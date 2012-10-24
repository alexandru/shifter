package shifter.migrations

import shifter.reflection._


abstract class Migrator(val packageName: String, val group: String) {
  def setup(): Unit
  def persistVersion(version: Int): Unit
  def currentVersion: Int

  def initMigration(cls: Class[Migration]) = 
    toInstance(cls)

  def withSession[A](f: MigratorSession => A) = {
    setup()

    if (currentVersion > 0)
      logger.info("Migration system initiated (starting from version "+currentVersion+")")
    else
      logger.info("Migration system initiated (starting from zero)")

    val ret = f(new MigratorSession(this))
    logger.info("  STOPPED (version " + currentVersion + ")")
    ret
  }

  lazy val logger = 
    org.slf4j.LoggerFactory.getLogger(group)
}
