package shifter.s3logger

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.util.concurrent.atomic.AtomicInteger


@RunWith(classOf[JUnitRunner])
class S3LoggerSuite extends FunSuite with TestHelpers {
  test("log 100000 items, forced rotate") {
    withLogger(60 * 60) { logger =>

      (0 until 10)
        .map(nr =>
          startThread(logger, "hello" + nr.toString, 10000))
        .foreach(_.join())

      assert(!logger.rotate(forced = false))
      assert(listKeys.length === 0)

      assert(!logger.rotate(forced = false))
      assert(listKeys.length === 0)

      assert(logger.rotate(forced = true))
      assert(listKeys.length === 1)

      val validLines = (0 until 10).map(nr => "hello" + nr.toString).toSet
      val regex = "^test/dt=\\d{4}-\\d{2}-\\d{2}/\\d{14}-\\w+\\.log\\.gz$"

      withSample { case (key, in) =>
        assert(key.matches(regex),
          "Key %s does not match the key file format: %s".format(key, regex))

        var line: Option[String] = None
        var count = 0

        do {
          line = Option(in.readLine())
          if (line.isDefined) {
            count += 1
            assert(validLines(line.get.trim))
          }
        } while (line.isDefined)

        assert(count === 10000 * 10)
      }
    }
  }

  test("log 30000 items, non-forced rotate, twice") {
    withLogger(1) { logger =>

      val thread1 = startThread(logger, "hello1", 10000)
      val thread2 = startThread(logger, "hello2", 10000)
      val thread3 = startThread(logger, "hello3", 10000)

      thread1.join()
      thread2.join()
      thread3.join()

      Thread.sleep(1000)
      assert(logger.rotate(forced = false))
      assert(listKeys.length === 1)

      val thread4 = startThread(logger, "hello1", 10000)
      val thread5 = startThread(logger, "hello2", 10000)
      val thread6 = startThread(logger, "hello3", 10000)

      thread4.join()
      thread5.join()
      thread6.join()

      Thread.sleep(1000)
      logger.rotate(forced = false)
      assert(listKeys.length === 2)

      val validLines = Set("hello1", "hello2", "hello3")
      val regex = "^test/dt=\\d{4}-\\d{2}-\\d{2}/\\d{14}-\\w+\\.log\\.gz$"

      withSample { case (key, in) =>
        assert(key.matches(regex),
          "Key %s does not match the key file format: %s".format(key, regex))

        var line: Option[String] = None
        var count = 0

        do {
          line = Option(in.readLine())
          if (line.isDefined) {
            count += 1
            assert(validLines(line.get.trim))
          }
        } while (line.isDefined)

        assert(count === 10000 * 3)
      }
    }
  }
}