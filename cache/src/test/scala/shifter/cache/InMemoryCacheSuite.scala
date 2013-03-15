package shifter.cache

import errors.NotFoundInCacheError
import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import concurrent.Await
import concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global



@RunWith(classOf[JUnitRunner])
class InMemoryCacheSuite extends FunSuite {

  test("add") {
    withCache() { cache =>
      cache.add("hello", Value("world"))

      val stored = cache.get[Value]("hello")
      assert(stored === Some(Value("world")))

      cache.add("hello", Value("changed"))
      val changed = cache.get[Value]("hello")
      assert(changed === Some(Value("world")))
    }
  }

  test("asyncAdd") {
    withCache() { cache =>
      val result = Await.result(cache.asyncAdd("hello", Value("world"), 2), 2.seconds)
      assert(result === true)

      val stored = cache.get[Value]("hello")
      assert(stored === Some(Value("world")))

      val result2 = Await.result(cache.asyncAdd("hello", Value("false")), 2.seconds)
      assert(result2 === false)

      val changed = cache.get[Value]("hello")
      assert(changed === Some(Value("world")))
    }
  }

  test("asyncTransformAndGet") {
    withCache() { cache =>
      val step1 = cache.asyncTransformAndGet("hello") { current: Option[String] =>
        assert(current === None)
        "world1"
      }

      assert(Await.result(step1, 1.second) === "world1")

      val step2 = cache.asyncTransformAndGet("hello") { current: Option[String] =>
        assert(current === Some("world1"))
        "world2"
      }

      assert(Await.result(step2, 1.second) === "world2")
    }
  }

  test("set") {
    withCache() { cache =>
      cache.set("mutable", Value("value1"))
      val stored = cache.get[Value]("mutable")
      assert(stored === Some(Value("value1")))

      cache.set("mutable", Value("value2"))
      val stored2 = cache.get[Value]("mutable")
      assert(stored2 === Some(Value("value2")))
    }
  }

  test("missing get") {
    withCache() { cache =>
      val stored = cache.get[Value]("missing-get")
      assert(stored === None)
    }
  }

  test("fireAdd") {
    withCache() { cache =>
      cache.fireAdd("hello", Value("world"))
      Thread.sleep(300)
      assert(cache.get[Value]("hello") === Some(Value("world")))

      cache.fireAdd("hello", Value("changed"))
      Thread.sleep(300)
      assert(cache.get[Value]("hello") === Some(Value("world")))
    }
  }

  test("fireSet") {
    withCache() { cache =>
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
    withCache() { cache =>
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
    withCache() { cache =>
      cache.set("hello", Value("world"))

      val stored = Await.result(cache.getAsyncOpt[Value]("hello"), 1.second)
      assert(stored === Some(Value("world")))

      val missing = Await.result(cache.getAsyncOpt[Value]("missing"), 1.second)
      assert(missing === None)
    }
  }

  test("getBulk") {
    withCache() { cache =>
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
    withCache() { cache =>
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

  test("maxElems") {
    withCache(3) { cache =>
      cache.set("key1", Value("value1"))
      cache.set("key2", Value("value2"))
      cache.set("key3", Value("value3"))
      cache.set("key4", Value("value4"))
      cache.set("key5", Value("value5"))

      val future = cache.getAsyncBulk(Seq("key1", "key2", "key3", "key4", "key5"))
        .map(_.asInstanceOf[Map[String, Value]])

      val values = Await.result(future, 1.second)
      assert(values.size === 3)
    }
  }


  val config = MemcachedConfiguration(
    addresses = "127.0.0.1:11211",
    authentication = None,
    keysPrefix = Some("my-tests"),
    protocol = Protocol.Binary
  )

  def withCache[T](maxElems: Int = 1000)(cb: Cache => T): T = {
    val cache = new InMemoryCache(maxElems)

    try {
      cb(cache)
    }
    finally {
      cache.shutdown()
    }
  }
}
