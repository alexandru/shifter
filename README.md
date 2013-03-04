# Shifter


Common infrastructure for the Epigrams web-services built on top of
the JVM and Scala.

## Usage from SBT

First add the proper resolvers:

```scala
resolvers += "Epigrams Releases" at "http://maven.epigrams.co/releases/"

resolvers += "Epigrams Snapshots" at "http://maven.epigrams.co/snapshots/"
```

For specifying all sub-projects as dependencies:

```scala
libraryDependencies += "shifter" %% "shifter" % "0.3.9"
```

For specifying individual subprojects:

```scala
libraryDependencies += "shifter" %% "shifter-web" % "0.3.9"
```

NOTE: this document may get out of date and the latest version may not
be the one specified.

## Usage from Maven

First add the repository resolvers:

```xml
<repositories>
  <repository>
    <id>Epigrams Releases</id>
    <url>http://maven.epigrams.co/releases/</url>
  </repository>
  <repository>
    <id>Epigrams Snapshots</id>
    <url>http://maven.epigrams.co/snapshots/</url>
  </repository>
</repositories>
```

For specifying all sub-projects as dependencies:

```xml
<dependency>
   <groupId>shifter</groupId>
   <artifactId>shifter_2.10</artifactId>
   <version>0.3.9</version>
</dependency>
```

For specifying specific sub-projects (e.g. shifter-db):

```xml
<dependency>
   <groupId>shifter</groupId>
   <artifactId>shifter-db_2.10</artifactId>
   <version>0.3.9</version>
</dependency>
```

NOTE: this document may get out of date and the latest version may not
be the one specified.
