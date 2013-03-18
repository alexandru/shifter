package shifter.cache

import memcached._
import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import concurrent.duration._
import shifter.concurrency._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.Some


@RunWith(classOf[JUnitRunner])
class MemcachedSuite extends FunSuite {
  implicit val timeout = 5.second

  test("add") {
    withCache("add") { cache =>
      val op1 = cache.add("hello", Value("world"), 5.seconds).await
      assert(op1 === true)

      val stored = cache.get[Value]("hello").await
      assert(stored === Some(Value("world")))

      val op2 = cache.add("hello", Value("changed"), 5.seconds).await
      assert(op2 === false)

      val changed = cache.get[Value]("hello").await
      assert(changed === Some(Value("world")))
    }
  }

  test("get") {
    withCache("get") { cache =>
      val value = cache.get[Value]("missing").await
      assert(value === None)

      try {
        cache[Value]("missingValue").await
        assert(condition = false, "apply() for missing value should yield exception")
      }
      catch {
        case ex: KeyNotInCacheException =>
          assert(ex.getMessage === "memcached.missingValue")
      }
    }
  }

  test("set") {
    withCache("set") { cache =>
      assert(cache.get[Value]("hello").await === None)

      cache.set("hello", Value("world"), 3.seconds).await
      assert(cache[Value]("hello").await === Value("world"))

      cache.set("hello", Value("changed"), 3.seconds).await
      assert(cache[Value]("hello").await === Value("changed"))

      Thread.sleep(3000)

      assert(cache.get[Value]("hello").await === None)
    }
  }

  test("delete") {
    withCache("delete") { cache =>
      cache.delete("hello").await
      assert(cache.get[Value]("hello").await === None)

      cache.set[Value]("hello", Value("world")).await
      assert(cache[Value]("hello").await === Value("world"))

      assert(cache.delete("hello").await === true)
      assert(cache.get[Value]("hello").await === None)

      assert(cache.delete("hello").await === false)
    }
  }

  test("bulkGet") {
    withCache("bulkGet") { cache =>
      cache.set("key1", Value("value1")).await
      cache.set("key2", Value("value2")).await

      val values = cache.getBulk(Seq("key1", "key2", "missing")).await
        .asInstanceOf[Map[String, Value]]

      assert(values.get("key1") === Some(Value("value1")))
      assert(values.get("key2") === Some(Value("value2")))
      assert(values.get("missing") === None)
    }
  }

  test("cas") {
    withCache("cas") { cache =>
      cache.delete("some-key").await
      assert(cache.get[Value]("some-key").await === None)

      // no can do
      assert(cache.cas("some-key", Some(Value("invalid")), Value("value1"), 15.seconds).await === false)
      assert(cache.get[Value]("some-key").await === None)

      // set to value1
      assert(cache.cas("some-key", None, Value("value1"), 5.seconds).await === true)
      assert(cache.get[Value]("some-key").await === Some(Value("value1")))

      // no can do
      assert(cache.cas("some-key", Some(Value("invalid")), Value("value1"), 15.seconds).await === false)
      assert(cache.get[Value]("some-key").await === Some(Value("value1")))

      // set to value2, from value1
      assert(cache.cas("some-key", Some(Value("value1")), Value("value2"), 15.seconds).await === true)
      assert(cache.get[Value]("some-key").await === Some(Value("value2")))

      // no can do
      assert(cache.cas("some-key", Some(Value("invalid")), Value("value1"), 15.seconds).await === false)
      assert(cache.get[Value]("some-key").await === Some(Value("value2")))

      // set to value3, from value2
      assert(cache.cas("some-key", Some(Value("value2")), Value("value3"), 15.seconds).await === true)
      assert(cache.get[Value]("some-key").await === Some(Value("value3")))
    }
  }

  test("transformAndGet") {
    withCache("transformAndGet") { cache =>
      cache.delete("some-key").await
      assert(cache.get[Value]("some-key").await === None)

      def incrementValue =
        cache.transformAndGet[Int]("some-key", 5.seconds) {
          case None => 1
          case Some(nr) => nr + 1
        }

      assert(incrementValue.await === 1)
      assert(incrementValue.await === 2)
      assert(incrementValue.await === 3)
      assert(incrementValue.await === 4)
      assert(incrementValue.await === 5)
    }
  }

  test("getAndTransform") {
    withCache("getAndTransform") { cache =>
      cache.delete("some-key").await
      assert(cache.get[Value]("some-key").await === None)

      def incrementValue =
        cache.getAndTransform[Int]("some-key", 5.seconds) {
          case None => 1
          case Some(nr) => nr + 1
        }

      assert(incrementValue.await === None)
      assert(incrementValue.await === Some(1))
      assert(incrementValue.await === Some(2))
      assert(incrementValue.await === Some(3))
      assert(incrementValue.await === Some(4))
      assert(incrementValue.await === Some(5))
      assert(incrementValue.await === Some(6))
    }
  }

  test("transformAndGet-concurrent") {
    withCache("transformAndGet") { cache =>
      cache.delete("some-key").await
      assert(cache.get[Value]("some-key").await === None)

      def incrementValue =
        cache.transformAndGet[Int]("some-key", 60.seconds) {
          case None => 1
          case Some(nr) => nr + 1
        }

      val seq = concurrent.Future.sequence((0 until 500).map(nr => incrementValue))
      seq.await(20.seconds)

      assert(cache.get[Int]("some-key").await === Some(500))
    }
  }

  test("getAndTransform-concurrent") {
    withCache("getAndTransform") { cache =>
      cache.delete("some-key").await
      assert(cache.get[Value]("some-key").await === None)

      def incrementValue =
        cache.getAndTransform[Int]("some-key", 60.seconds) {
          case None => 1
          case Some(nr) => nr + 1
        }

      val seq = concurrent.Future.sequence((0 until 500).map(nr => incrementValue))
      seq.await(20.seconds)

      assert(cache.get[Int]("some-key").await === Some(500))
    }
  }

  val config = Configuration(
    addresses = "127.0.0.1:11211",
    authentication = None,
    keysPrefix = Some("my-tests"),
    protocol = Protocol.Binary,
    failureMode = FailureMode.Retry
  )

  def withCache[T](prefix: String)(cb: Cache => T): T = {
    System.setProperty("net.spy.log.LoggerImpl", "net.spy.memcached.compat.log.SunLogger")
    java.util.logging.Logger.getLogger("net.spy.memcached")
      .setLevel(java.util.logging.Level.WARNING)

    val cache = Memcached(
      config.copy(keysPrefix = config.keysPrefix.map(s => s + "-" + prefix)))

    try {
      cb(cache)
    }
    finally {
      cache.shutdown()
    }
  }
}
