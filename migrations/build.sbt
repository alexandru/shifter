name := "shifter-migrations"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.6.4",
  "commons-io" % "commons-io" % "2.4" % "test",
  "hsqldb" % "hsqldb" % "1.8.0.10" % "test",
  "mysql" % "mysql-connector-java" % "5.1.20" % "test",
  "org.scalatest" %% "scalatest" % "1.8" % "test",
  "junit" % "junit" % "4.10" % "test"
)
