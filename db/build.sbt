name := "shifter-db"

libraryDependencies ++= Seq(
  "org.scala-lang.virtualized" % "scala-reflect" % "2.10.1",
  "hsqldb" % "hsqldb" % "1.8.0.10" % "test",
  "mysql" % "mysql-connector-java" % "5.1.20" % "test",
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "junit" % "junit" % "4.10" % "test"
)
