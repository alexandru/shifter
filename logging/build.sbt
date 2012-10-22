name := "shifter-logging"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.6.4",
  "org.slf4j" % "jul-to-slf4j" % "1.6.4",
  "org.slf4j" % "log4j-over-slf4j" % "1.6.4",
  "ch.qos.logback" % "logback-core" % "1.0.4",
  "ch.qos.logback" % "logback-classic" % "1.0.4",
  "org.scalatest" %% "scalatest" % "1.8" % "test",
  "junit" % "junit" % "4.10" % "test"
)
