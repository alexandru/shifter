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
dependencies += "shifter" %% "shifter-cache" % "0.3.64-SNAPSHOT"
```

## Usage

Take a look at the [Cache](src/main/scala/shifter/cache/Cache.scala)
trait.

Note that every method on that trait is non-blocking and returns a
[Future](http://docs.scala-lang.org/sips/pending/futures-promises.html)
instead of a concrete answer.

### Creating an Instance

```scala
import shifter.cache._
import shifter.cache.memcached._

// we need an implicit execution context when creating an instance
import concurrent.ExecutionContext.Implicits.global

val config = Configuration(
  addresses = "127.0.0.1:11211",
  keysPrefix = Some("my-tests"),
  protocol = Protocol.Binary,
  failureMode = FailureMode.Retry
)

val cache = Cache(config)
```

The implementation uses non-blocking I/O, keeps connections opened and
uses its own threadpool, so it's a good idea to shut it down when you
no longer need it:

```
cache.shutdown()
```

The implementation is thread-safe and it is highly recommended to use
a single instance amongst multiple threads. In case of web servers,
instantiate a single instance when the process starts and shut it down
only when the process exists or just don't shut it down if it's a true
singleton.

### Adding a value for a key in case the case doesn't exist

```scala
import concurrent.duration._

cache.add("some-key", "hello", 10.minutes) map {
  case true =>
    println("Key was added")
  case false =>
    println("Warn: Key is already present")
}
```

### Setting the value for a key

```scala
cache.set("some-key", "hello", 10.minutes)
```

Note that infinite durations are supported:

```scala
import concurrent.duration._

cache.set("some-key", "hello", Duration.Inf)
```

### Deleting a key from the cache store

```scala
cache.delete("some-key") map {
  case true =>
    println("A key was deleted")
  case false =>
    println("No key was present so nothing deleted")
}
```

### Fetching values 

```scala
val str: Future[String] = 
  cache[String]("some-key")
    .map(value => value + ", world")
    .recover {
      case _: KeyNotInCacheException =>
	    "hello, world"
    }
```

If you don't want to deal with `KeyNotInCacheException` exception, you
can get an `Option[T]` instead by using `get()` instead of `apply()`:

```scala
val str = 
  cache.get[String]("some-key") map {
    case Some(str) => value + ", world"
	case None => "hello, world"
  }
```

If instead of dealing with `Option[T]` you want to get a default in
case the key is missing:

```scala
val str: Future[String] = cache.getOrElse("some-key", "hello")
```

If you want to fetch multiple keys in bulk and get a
`Map[String, Any]` back:

```scala
val result: Future[Map[String, Any]] = 
  cache.getBulk("key1" :: "key2" :: "key3" :: Nil)
```

### Compare and Set

```scala
val success: Future[Boolean] = cache.cas(
  key = "my-name",
  expecting = Some("Alex"),
  newValue = "Chris",
  exp = Duration.Inf
)  
```

Working with the `cas()` method may be too difficult. Much better is
to use either `transformAndGet` for transforming the existing value,
returning the new stored value for the indicated key. This way you can
implement an atomic counter for example:

```scala
val generatedID: Future[Int] = cache.transformAndGet[Int]("ids") { 
  case None => 0
  case Some(current) =>
    current + 1
}
```

You can also use `getAndTransform` for transforming the existing value
associated with a key, returning the old value that was in place
before the transformation occurred:

```scala
val generatedID: Future[Option[Int]] = cache.getAndTransform[Int]("ids") { 
  case None => 0
  case Some(current) =>
    current + 1
}
```

Note that `getAndTransform` can return `None` in case the key was
missing from our cache.

*Carefull* about these compare-and-set methods with Memcached, as
contention can kill you. These methods don't work so well in a highly
concurrent environment.

