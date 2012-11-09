import sbt._
import Keys._

object ShifterBuild extends Build {
    lazy val root = Project(
      id = "shifter",
      base = file("."),

      settings = Project.defaultSettings ++ Seq(
	name := "shifter",

        organization in ThisBuild := "com.bionicspirit",

	version in ThisBuild := "0.2.6.3-SNAPSHOT",

	scalaVersion in ThisBuild := "2.9.2",

	crossScalaVersions in ThisBuild := Seq("2.9.1", "2.9.2"),

	scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation"),

	licenses in ThisBuild := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php")),

	homepage in ThisBuild := Some(url("http://github.com/alexandru/shifter")),

        pomExtra in ThisBuild := (
          <scm>
            <url>git@github.com:alexandru/shifter.git</url>
            <connection>scm:git:git@github.com:alexandru/shifter.git</connection>
          </scm>
          <developers>
            <developer>
              <id>alex_ndc</id>
              <name>Alexandru Nedelcu</name>
              <url>http://bionicspirit.com</url>
            </developer>
          </developers>),

	resolvers in ThisBuild ++= Seq(
	  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/")
      )
    ) 
    .aggregate(core, validations, config, logging, db, migrations, misc) 
    .dependsOn(core, validations, config, logging, db, migrations)

    lazy val core = Project(
      id = "shifter-core",
      base = file("core"))

    lazy val validations = Project(
      id = "shifter-validations",
      base = file("validations")) dependsOn(core, db)

    lazy val logging = Project(
      id = "shifter-logging",
      base = file("logging")) dependsOn(core)

    lazy val config = Project(
      id = "shifter-config",
      base = file("config")) dependsOn(core)

    lazy val db = Project(
      id = "shifter-db",
      base = file("db")) dependsOn(core)

    lazy val migrations = Project(
      id = "shifter-migrations",
      base = file("migrations")) dependsOn(core, db)

    lazy val misc = Project(
      id = "shifter-misc",
      base = file("misc")) dependsOn(core)
}
