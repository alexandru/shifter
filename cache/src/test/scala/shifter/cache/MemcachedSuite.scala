package shifter.cache

import memcached._
import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import concurrent.duration._
import shifter.concurrency.extensions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.Some
import shifter.models.Impression
import java.io.{ObjectOutputStream, ByteArrayOutputStream}


@RunWith(classOf[JUnitRunner])
class MemcachedSuite extends FunSuite {
  implicit val timeout = 5.second

  test("add") {
    withCache("add") { cache =>
      val op1 = cache.asyncAdd("hello", Value("world"), 5.seconds).await
      assert(op1 === true)

      val stored = cache.asyncGet[Value]("hello").await
      assert(stored === Some(Value("world")))

      val op2 = cache.asyncAdd("hello", Value("changed"), 5.seconds).await
      assert(op2 === false)

      val changed = cache.asyncGet[Value]("hello").await
      assert(changed === Some(Value("world")))
    }
  }

  test("add-null") {
    withCache("add-null") { cache =>
      val op1 = cache.asyncAdd("hello", null, 5.seconds).await
      assert(op1 === false)

      val stored = cache.asyncGet[Value]("hello").await
      assert(stored === None)
    }
  }

  test("get") {
    withCache("get") { cache =>
      val value = cache.asyncGet[Value]("missing").await
      assert(value === None)
    }
  }

  test("set") {
    withCache("set") { cache =>
      assert(cache.asyncGet[Value]("hello").await === None)

      cache.asyncSet("hello", Value("world"), 3.seconds).await
      assert(cache.get[Value]("hello") === Some(Value("world")))

      cache.asyncSet("hello", Value("changed"), 3.seconds).await
      assert(cache.get[Value]("hello") === Some(Value("changed")))

      Thread.sleep(3000)

      assert(cache.asyncGet[Value]("hello").await === None)
    }
  }

  test("set-null") {
    withCache("set-null") { cache =>
      val op1 = cache.asyncAdd("hello", null, 5.seconds).await
      assert(op1 === false)

      val stored = cache.asyncGet[Value]("hello").await
      assert(stored === None)
    }
  }

  test("delete") {
    withCache("delete") { cache =>
      cache.asyncDelete("hello").await
      assert(cache.asyncGet[Value]("hello").await === None)

      cache.asyncSet[Value]("hello", Value("world")).await
      assert(cache.get[Value]("hello") === Some(Value("world")))

      assert(cache.asyncDelete("hello").await === true)
      assert(cache.asyncGet[Value]("hello").await === None)

      assert(cache.asyncDelete("hello").await === false)
    }
  }

  test("bulkGet") {
    withCache("bulkGet") { cache =>
      cache.asyncSet("key1", Value("value1")).await
      cache.asyncSet("key2", Value("value2")).await

      val values = cache.asyncBulk(Seq("key1", "key2", "missing")).await
        .asInstanceOf[Map[String, Value]]

      assert(values.get("key1") === Some(Value("value1")))
      assert(values.get("key2") === Some(Value("value2")))
      assert(values.get("missing") === None)
    }
  }

  test("cas") {
    withCache("cas") { cache =>
      cache.asyncDelete("some-key").await
      assert(cache.asyncGet[Value]("some-key").await === None)

      // no can do
      assert(cache.cas("some-key", Some(Value("invalid")), Value("value1"), 15.seconds).await === false)
      assert(cache.asyncGet[Value]("some-key").await === None)

      // set to value1
      assert(cache.cas("some-key", None, Value("value1"), 5.seconds).await === true)
      assert(cache.asyncGet[Value]("some-key").await === Some(Value("value1")))

      // no can do
      assert(cache.cas("some-key", Some(Value("invalid")), Value("value1"), 15.seconds).await === false)
      assert(cache.asyncGet[Value]("some-key").await === Some(Value("value1")))

      // set to value2, from value1
      assert(cache.cas("some-key", Some(Value("value1")), Value("value2"), 15.seconds).await === true)
      assert(cache.asyncGet[Value]("some-key").await === Some(Value("value2")))

      // no can do
      assert(cache.cas("some-key", Some(Value("invalid")), Value("value1"), 15.seconds).await === false)
      assert(cache.asyncGet[Value]("some-key").await === Some(Value("value2")))

      // set to value3, from value2
      assert(cache.cas("some-key", Some(Value("value2")), Value("value3"), 15.seconds).await === true)
      assert(cache.asyncGet[Value]("some-key").await === Some(Value("value3")))
    }
  }

  test("transformAndGet") {
    withCache("transformAndGet") { cache =>
      cache.asyncDelete("some-key").await
      assert(cache.asyncGet[Value]("some-key").await === None)

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
      cache.asyncDelete("some-key").await
      assert(cache.asyncGet[Value]("some-key").await === None)

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
      cache.asyncDelete("some-key").await
      assert(cache.asyncGet[Value]("some-key").await === None)

      def incrementValue =
        cache.transformAndGet[Int]("some-key", 60.seconds) {
          case None => 1
          case Some(nr) => nr + 1
        }

      val seq = concurrent.Future.sequence((0 until 500).map(nr => incrementValue))
      seq.await(20.seconds)

      assert(cache.asyncGet[Int]("some-key").await === Some(500))
    }
  }

  test("getAndTransform-concurrent") {
    withCache("getAndTransform") { cache =>
      cache.asyncDelete("some-key").await
      assert(cache.asyncGet[Value]("some-key").await === None)

      def incrementValue =
        cache.getAndTransform[Int]("some-key", 60.seconds) {
          case None => 1
          case Some(nr) => nr + 1
        }

      val seq = concurrent.Future.sequence((0 until 500).map(nr => incrementValue))
      seq.await(20.seconds)

      assert(cache.asyncGet[Int]("some-key").await === Some(500))
    }
  }

  test("big-instance-1") {
    withCache("big-instance-1") { cache =>
      val impression = shifter.models.bigInstance
      cache.set(impression.uuid, impression, 60.seconds)
      assert(cache.get[Impression](impression.uuid) === Some(impression))
    }
  }


  test("big-instance-1-manual") {
    withCache("big-instance-1-manual") { cache =>
      val byteOut = new ByteArrayOutputStream()
      val objectOut = new ObjectOutputStream(byteOut)

      val impression = shifter.models.bigInstance
      objectOut.writeObject(impression)
      val byteArray = byteOut.toByteArray

      cache.set(impression.uuid, byteArray, 60.seconds)

      val inBytes = cache.get[Array[Byte]](impression.uuid)
      assert(inBytes.isDefined)
      assert(inBytes.get.length == byteArray.length)
    }
  }


  test("big-instance-2") {
    withCache("big-instance-2") { cache =>
      val impression = shifter.models.bigInstance2
      cache.set(impression.uuid, impression, 60.seconds)
      assert(cache.get[Impression](impression.uuid) === Some(impression))
    }
  }

  test("big-instance-3") {
    withCache("big-instance-3") { cache =>
      val impression = shifter.models.bigInstance
      val future = cache.asyncSet(impression.uuid, impression, 60.seconds) flatMap { _ =>
        cache.asyncGet[Impression](impression.uuid)
      }

      val result = future.await
      assert(result === Some(impression))\
    }
  }

  val config = Configuration(
    addresses = "127.0.0.1:11211",
    authentication = None,
    keysPrefix = Some("my-tests"),
    protocol = Protocol.Binary,
    failureMode = FailureMode.Retry,
    operationTimeout = 15.seconds
  )

  def withCache[T](prefix: String)(cb: Cache => T): T = {
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
