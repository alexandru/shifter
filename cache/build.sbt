name := "shifter-cache"

libraryDependencies ++= Seq(
  "spy" % "spymemcached" % "2.8.4",
  "org.scala-stm" %% "scala-stm" % "0.7",
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "junit" % "junit" % "4.10" % "test"
)
