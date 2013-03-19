package shifter.cache

import memcached.{FailureMode, Protocol, Configuration, Memcached}
import concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import concurrent.Future
import shifter.concurrency._


object Benchmark extends App {
  val cache = Memcached(Configuration(
    addresses = "127.0.0.1:11211",
    authentication = None,
    keysPrefix = Some("benchmark"),
    protocol = Protocol.Binary,
    failureMode = FailureMode.Redistribute,
    operationTimeout = 1.hour
  ))

  try {

    for (i <- 0 until 10000) {
      val allFutures = (0 until 10).map { nr =>
        val key = "key" + nr.toString

        cache.set(key, nr, 60.minutes).flatMap { _ =>
          val reads = (0 until 100).map { nr =>
            cache.get[Int](key).map{ value =>
              val testKey = "key" + value.getOrElse("<>").toString
              assert(testKey == key, "test failed for -> " + key + ": " + testKey)
              value.get
            }
          }

          Future.sequence(reads)
        }
      }

      Future.sequence(allFutures).await(30.minutes)
    }

    Console.readLine()
  }
  finally {
    cache.shutdown()
  }
}
