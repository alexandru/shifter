import sbt._
import Keys._

object ShifterBuild extends Build {
  lazy val root = Project(
    id = "shifter",
    base = file("."),

    settings = Project.defaultSettings ++ Seq(
      name := "shifter",

      organization in ThisBuild := "shifter",

      version in ThisBuild := "0.3.21",

      scalaVersion in ThisBuild := "2.10.0",

      crossScalaVersions in ThisBuild := Seq("2.10.0"),

      compileOrder in ThisBuild := CompileOrder.JavaThenScala,

      scalacOptions in ThisBuild ++= Seq(
        "-unchecked", "-deprecation", "-feature",
        "-language:existentials",
        "-language:implicitConversions"
      ),

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
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
        "Spray Releases" at "http://repo.spray.io",
        "Spy" at "http://files.couchbase.com/maven2/"
      )
    )
  )
  .aggregate(core, db, migrations, cache, httpClient, geoip, web)
  .dependsOn(core, db, migrations, cache, httpClient, geoip, web)

  lazy val core = Project(
    id = "shifter-core",
    base = file("core"))

  lazy val db = Project(
    id = "shifter-db",
    base = file("db")) dependsOn (core)

  lazy val migrations = Project(
    id = "shifter-migrations",
    base = file("migrations")) dependsOn(core, db)

  lazy val httpClient = Project(
    id = "shifter-http-client",
    base = file("http-client")) dependsOn (core)

  lazy val cache = Project(
    id = "shifter-cache",
    base = file("cache")) dependsOn (core)

  lazy val geoip = Project(
    id = "shifter-geoip",
    base = file("geoip")) dependsOn (core)

  lazy val web = Project(
    id = "shifter-web",
    base = file("web")) dependsOn (core, geoip, httpClient)
}
