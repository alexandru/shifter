# Shifter

Shifter is my dumping-ground for some reusable code I write, in an
attempt to put together a common set of tools for building
web-services in Scala, while staying productive.

## Installation

I publish the artifacts of this in my own Maven repo. Using it means
adding a repository resolver.

### As a Maven Dependency

For the repo resolver:

    <repositories>
      <repository>
        <id>BionicSpirit Releases</id>
        <url>http://maven.bionicspirit.com/releases/</url>
      </repository>
	  
      <repository>
        <id>BionicSpirit Snapshots</id>
        <url>http://maven.bionicspirit.com/snapshots/</url>
      </repository>
    </repositories>

For the dependency:

    <dependencies>
	    <!-- ... -->
        <dependency>
            <groupId>com.bionicspirit</groupId>
            <artifactId>shifter_2.9.2</artifactId>
            <version>0.2.1</version>
        </dependency>
	    <!-- ... -->
    <dependencies>
    
### Usage with SBT

For the repo resolver:

    resolvers += "BionicSpirit Releases" at "http://maven.bionicspirit.com/releases/"

    resolvers += "BionicSpirit Snapshots at "http://maven.bionicspirit.com/snapshots/"

For the dependency:
    
    libraryDependencies += "com.bionicspirit" %% "shifter" % "0.2.1"
