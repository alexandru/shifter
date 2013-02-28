name := "shifter-web"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.6.4",
  "io.spray" %%  "spray-json" % "1.2.3",
  "commons-codec" % "commons-codec" % "1.5",
  "org.eclipse.jetty" % "jetty-webapp" % "8.1.9.v20130131",
  "org.eclipse.jetty" % "jetty-server" % "8.1.9.v20130131",
  "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "provided;test;runtime" artifacts (Artifact("javax.servlet", "jar", "jar")),
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "junit" % "junit" % "4.10" % "test"
)
