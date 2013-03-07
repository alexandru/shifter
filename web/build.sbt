name := "shifter-web"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.0.0",
  "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
  "ch.qos.logback" % "logback-classic" % "1.0.6",
  "com.yammer.metrics" % "metrics-core" % "2.2.0",
  "com.yammer.metrics" % "metrics-jetty" % "2.2.0",
  "com.yammer.metrics" % "metrics-servlet" % "2.2.0",
  "com.yammer.metrics" % "metrics-graphite" % "2.2.0",
  "io.spray" %% "twirl-api" % "0.6.1",
  "io.spray" %%  "spray-json" % "1.2.3",
  "org.scala-stm" %% "scala-stm" % "0.7",
  "commons-io" % "commons-io" % "1.3.2",
  "commons-fileupload" % "commons-fileupload" % "1.2.2",
  "org.eclipse.jetty" % "jetty-webapp" % "8.1.9.v20130131",
  "org.eclipse.jetty" % "jetty-server" % "8.1.9.v20130131",
  "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "provided;test;runtime" artifacts (Artifact("javax.servlet", "jar", "jar")),
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "junit" % "junit" % "4.10" % "test"
)

