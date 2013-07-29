name := "shifter-migrations"

libraryDependencies ++= Seq(
  "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
  "commons-io" % "commons-io" % "2.4" % "test",
  "hsqldb" % "hsqldb" % "1.8.0.10" % "test",
  "mysql" % "mysql-connector-java" % "5.1.20" % "test",
  "ch.qos.logback" % "logback-classic" % "1.0.6" % "test",
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "junit" % "junit" % "4.10" % "test"
)
