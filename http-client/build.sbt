name := "shifter-http-client"

libraryDependencies ++= Seq(
  "com.ning" % "async-http-client" % "1.7.14",
  "ch.qos.logback" % "logback-classic" % "1.0.6" % "test",
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "junit" % "junit" % "4.10" % "test"
)
