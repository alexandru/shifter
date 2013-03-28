package shifter.s3logger.async

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class S3AsyncLoggerSuite extends FunSuite with TestHelpers {
  test("log 10000 items, single thread, forced rotate") {
    withAsyncLogger(60 * 60) { s3logger =>
      deleteAllKeys()
      assert(listKeys.length === 0)

      for (i <- 1 to 10000)
        s3logger.write("hello.%d\n".format(i))

      val statsFirst = s3logger.rotate(forced = false)
      assert(statsFirst.isEmpty, "Should not have uploaded: %s".format(statsFirst.toString()))
      assert(listKeys.isEmpty, "There should be no keys in our Amazon S3 bucket")

      val stats = s3logger.rotate(forced = true)
      assert(stats.length === 1)
      assert(listKeys.length === 1)
      assert(stats.head.fileSizeInBytes > 20 * 1024,
        "Uploaded file size should be about 20 Kb, but is reported as being %.2f Kb".format(
          stats.head.fileSizeInBytes / 1024.0
        ))

      val regex = "^test/dt=\\d{4}-\\d{2}-\\d{2}/\\d{14}-\\w+\\.log\\.gz$"

      withSample { case (key, in) =>
        assert(key === stats.head.s3Key)
        assert(key.matches(regex),
          "Key %s does not match the key file format: %s".format(key, regex))

        for (i <- 1 to 10000) {
          val line = Option(in.readLine())
          assert(line.isDefined)
          assert(line.get.trim === "hello." + i.toString)
        }

        assert(Option(in.readLine()) === None)
      }
    }
  }

  test("log 30000 items, multi-threaded, non-forced rotate, twice") {
    withAsyncLogger(1) { s3logger =>
      assert(listKeys.length === 0)

      (1 to 3).map(nr => startThread(s3logger, "hello" + nr.toString, 10000))
        .toList
        .foreach(_.get())

      Thread.sleep(1200)
      assert(s3logger.rotate(forced = false).length === 1)
      assert(listKeys.length === 1)

      (4 to 6).map(nr => startThread(s3logger, "hello" + nr.toString, 10000))
        .toList
        .foreach(_.get())

      Thread.sleep(1200)
      assert(s3logger.rotate(forced = false).length === 1)
      assert(listKeys.length === 2)

      val validLines = (1 to 6).map(nr => "hello" + nr.toString).toSet
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