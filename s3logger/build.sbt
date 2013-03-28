name := "shifter-s3logger"

libraryDependencies ++= Seq(  
  "com.amazonaws" % "aws-java-sdk" % "1.3.10",
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "ch.qos.logback" % "logback-classic" % "1.0.6" % "test",
  "junit" % "junit" % "4.10" % "test"
)
