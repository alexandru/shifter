import sbt._
import Keys._

object ShifterBuild extends Build {

  lazy val projectVersion = {
    import scala.io.Source
    Source.fromFile("VERSION").getLines.mkString("\n").trim
  }

  lazy val root = Project(
    id = "shifter",
    base = file("."),

    settings = Project.defaultSettings ++ Seq(
      name := "shifter",

      organization in ThisBuild := "shifter",

      version in ThisBuild := projectVersion,

      scalaVersion in ThisBuild := "2.10.2",

      compileOrder in ThisBuild := CompileOrder.JavaThenScala,

      scalacOptions in ThisBuild ++= Seq(
        "-unchecked", "-deprecation", "-feature",
        "-target:jvm-1.6"
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
        "Spy" at "http://files.couchbase.com/maven2/",
        "BionicSpirit Releases" at "http://maven.bionicspirit.com/releases/",
        "BionicSpirit Snapshots" at "http://maven.bionicspirit.com/snapshots/"
      )
    )
  )
  .aggregate(core, db, migrations, json, httpClient, geoip, webApi, webJetty8, webJetty9, webSample)
  .dependsOn(core, db, migrations, json, httpClient, geoip, webApi)

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

  lazy val json = Project(
    id = "shifter-json",
    base = file("json")) dependsOn (core)

  lazy val geoip = Project(
    id = "shifter-geoip",
    base = file("geoip")) dependsOn (core)

  lazy val webApi = Project(
    id = "shifter-web-api",
    base = file("web-api")) dependsOn (core, geoip, httpClient)

  lazy val webJetty8 = Project(
    id = "shifter-web-jetty8",
    base = file("web-jetty8")) dependsOn (webApi)

  lazy val webJetty9 = Project(
    id = "shifter-web-jetty9",
    base = file("web-jetty9")) dependsOn (webApi)

  lazy val webSample = Project(
    id = "shifter-web-sample",
    base = file("web-sample")) dependsOn (webJetty9)
}
