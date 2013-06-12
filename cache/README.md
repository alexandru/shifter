# shifter-cache

Reactive Memcached client for Scala that's built on top of SpyMemcached.

Depends on Scala 2.10

## Usage from SBT

Add these resolvers:

```scala
resolvers ++= Seq(
  // just in case you don't have it already
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  // for SpyMemcached (shifter-cache dependency)
  "Spy" at "http://files.couchbase.com/maven2/",
  // for Shifter
  "BionicSpirit Releases" at "http://maven.bionicspirit.com/releases/",
  "BionicSpirit Snapshots" at "http://maven.bionicspirit.com/snapshots/"
)
```

Add the dependency:

```scala
dependencies += "shifter" %% "shifter-cache" % "0.3.113-SNAPSHOT"
```

## Usage

Take a look at the [Cache](src/main/scala/shifter/cache/Cache.scala)
trait.

Checkout the [API docs](http://shifter.bionicspirit.com/api/current/cache/#shifter.cache.Cache).

Work on documentation is in progress.
