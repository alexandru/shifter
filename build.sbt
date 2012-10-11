organization := "com.bionicspirit"

name := "scala-shifter"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.9.2"

licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))

homepage := Some(url("http://github.com/alexandru/scala-shifter"))

libraryDependencies ++= Seq(
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
