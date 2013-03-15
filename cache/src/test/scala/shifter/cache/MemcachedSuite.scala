package shifter.cache

import errors.NotFoundInCacheError
import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import concurrent.{Future, Await}
import concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


@RunWith(classOf[JUnitRunner])
class MemcachedSuite extends FunSuite {

  test("add") {
    withCache("add") { cache =>
      cache.add("hello", Value("world"))

      val stored = cache.get[Value]("hello")
      assert(stored === Some(Value("world")))

      cache.add("hello", Value("changed"))
      val changed = cache.get[Value]("hello")
      assert(changed === Some(Value("world")))
    }
  }

  test("asyncAdd") {
    withCache("asyncAdd") { cache =>
      val ts = System.currentTimeMillis()

      val result = Await.result(cache.asyncAdd("hello", Value("world"), 2), 2.seconds)
      assert(result === true)

      val stored = cache.get[Value]("hello")
      assert(stored === Some(Value("world")))

      val result2 = Await.result(cache.asyncAdd("hello", Value("false"), 2), 2.seconds)
      assert(result2 === false)

      val changed = cache.get[Value]("hello")
      assert(changed === Some(Value("world")))

      // testing expiry
      val sleepMillis = 2.seconds.toMillis - (System.currentTimeMillis() - ts)
      if (sleepMillis > 0) Thread.sleep(sleepMillis)

      assert(cache.get[Value]("hello") === None)
    }
  }

  test("asyncTransformAndGet") {
    withCache("asyncTransformAndGet") { cache =>
      val step1 = cache.asyncTransformAndGet("hello", 3) { current: Option[String] =>
        assert(current === None)
        "world1"
      }

      assert(Await.result(step1, 1.second) === "world1")

      val step2 = cache.asyncTransformAndGet("hello", 3) { current: Option[String] =>
        assert(current === Some("world1"))
        "world2"
      }

      assert(Await.result(step2, 1.second) === "world2")
    }
  }

  test("asyncTransformAndGet highly concurrent") {
    withCache("asyncTransformAndGetConcurrent") { cache =>
      val list = (0 until 1000).map(nr => cache.asyncTransformAndGet[Int]("value", 5) {
        case None => 0
        case Some(i) => i + 1
      })

      val future = Future.sequence(list)
      val result = Await.result(future, 10.seconds)

      val sorted = result.sorted
      assert(sorted.length === 1000)

      sorted.zip(0 until 1000).foreach {
        case (a, b) => assert(a === b)
      }
    }
  }

  test("set") {
    withCache("set") { cache =>
      cache.set("mutable", Value("value1"))
      val stored = cache.get[Value]("mutable")
      assert(stored === Some(Value("value1")))

      cache.set("mutable", Value("value2"))
      val stored2 = cache.get[Value]("mutable")
      assert(stored2 === Some(Value("value2")))
    }
  }

  test("missing get") {
    withCache("missing-get") { cache =>
      val stored = cache.get[Value]("missing-get")
      assert(stored === None)
    }
  }

  test("fireAdd") {
    withCache("fireAdd") { cache =>
      cache.fireAdd("hello", Value("world"))
      Thread.sleep(300)
      assert(cache.get[Value]("hello") === Some(Value("world")))

      cache.fireAdd("hello", Value("changed"))
      Thread.sleep(300)
      assert(cache.get[Value]("hello") === Some(Value("world")))
    }
  }

  test("fireSet") {
     withCache("fireSet") { cache =>
       cache.fireSet("mutable", Value("value1"))
       Thread.sleep(300)
       val stored = cache.get[Value]("mutable")
       assert(stored === Some(Value("value1")))

       cache.fireSet("mutable", Value("value2"))
       Thread.sleep(300)
       val stored2 = cache.get[Value]("mutable")
       assert(stored2 === Some(Value("value2")))
     }
  }

  test("getAsync") {
    withCache("getAsync") { cache =>
      cache.set("hello", Value("world"))

      val stored = Await.result(cache.getAsync[Value]("hello"), 1.second)
      assert(stored === Value("world"))

      try {
        Await.result(cache.getAsync[Value]("missing"), 1.second)
        assert(condition = false, "Shouldn't be successful")
      }
      catch {
        case ex: Exception =>
          assert(ex.isInstanceOf[NotFoundInCacheError], "Not the right exception")
      }
    }
  }

  test("getAsyncOpt") {
    withCache("getAsyncOpt") { cache =>
      cache.set("hello", Value("world"))

      val stored = Await.result(cache.getAsyncOpt[Value]("hello"), 1.second)
      assert(stored === Some(Value("world")))

      val missing = Await.result(cache.getAsyncOpt[Value]("missing"), 1.second)
      assert(missing === None)
    }
  }

  test("getBulk") {
    withCache("getBulk") { cache =>
      cache.set("key1", Value("value1"))
      cache.set("key2", Value("value2"))

      val values = cache.getBulk(Seq("key1", "key2", "missing")).asInstanceOf[Map[String, Value]]

      assert(values.size === 2)
      assert(values.get("key1") === Some(Value("value1")))
      assert(values.get("key2") === Some(Value("value2")))
      assert(values.get("missing") === None)
    }
  }

  test("getBulkAsync") {
    withCache("getBulk") { cache =>
      cache.set("key1", Value("value1"))
      cache.set("key2", Value("value2"))

      val future = cache.getAsyncBulk(Seq("key1", "key2", "missing"))
        .map(_.asInstanceOf[Map[String, Value]])

      val values = Await.result(future, 1.second)

      assert(values.size === 2)
      assert(values.get("key1") === Some(Value("value1")))
      assert(values.get("key2") === Some(Value("value2")))
      assert(values.get("missing") === None)
    }
  }

  val config = MemcachedConfiguration(
    addresses = "127.0.0.1:11211",
    authentication = None,
    keysPrefix = Some("my-tests"),
    protocol = Protocol.Binary
  )

  def withCache[T](prefix: String)(cb: Cache => T): T = {
    val cache = new Memcached(
      config.copy(keysPrefix = config.keysPrefix.map(s => s + "-" + prefix)))

    try {
      cb(cache)
    }
    finally {
      cache.shutdown()
    }
  }
}
