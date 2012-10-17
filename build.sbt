organization := "com.bionicspirit"

name := "scala-shifter"

version := "0.2"

scalaVersion := "2.9.2"

scalacOptions ++= Seq("-unchecked", "-deprecation")

licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))

homepage := Some(url("http://github.com/alexandru/scala-shifter"))

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "org.yaml" % "snakeyaml" % "1.10",
  "hsqldb" % "hsqldb" % "1.8.0.10" % "test",
  "org.apache.derby" % "derby" % "10.9.1.0" % "test",
  "com.ning" % "async-http-client" % "1.7.5" % "test",
  "com.typesafe.akka" % "akka-actor" % "2.0.3" % "test",
  "mysql" % "mysql-connector-java" % "5.1.20" % "test",
  "commons-io" % "commons-io" % "2.4" % "test",
  "org.scalatest" %% "scalatest" % "1.8" % "test",
  "junit" % "junit" % "4.10" % "test"
)

pomExtra := (
  <scm>
    <url>git@github.com:alexandru/scala-shifter.git</url>
    <connection>scm:git:git@github.com:alexandru/scala-shifter.git</connection>
  </scm>
  <developers>
    <developer>
      <id>alex_ndc</id>
      <name>Alexandru Nedelcu</name>
      <url>http://bionicspirit.com</url>
    </developer>
  </developers>)
