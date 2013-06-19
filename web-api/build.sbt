name := "shifter-web-api"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.0.0",
  "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
  "ch.qos.logback" % "logback-classic" % "1.0.6",
  "io.spray" %% "twirl-api" % "0.6.1",
  "commons-io" % "commons-io" % "1.3.2",
  "commons-fileupload" % "commons-fileupload" % "1.2.2",
  "commons-codec" % "commons-codec" % "1.5",
  "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "provided;test" artifacts (Artifact("javax.servlet", "jar", "jar")),
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "junit" % "junit" % "4.10" % "test"
)

