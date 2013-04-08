name := "shifter-cache"

libraryDependencies ++= Seq(
  "spy" % "spymemcached" % "2.8.4",
  "org.slf4j" % "slf4j-api" % "1.7.4",
  "ch.qos.logback" % "logback-classic" % "1.0.6" % "test",
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "junit" % "junit" % "4.10" % "test"
)
