package shifter.s3logger.playground

import shifter.io.{AsyncBufferedOutputStream, AsyncFileOutputStream}
import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import shifter.concurrency.extensions._
import concurrent.duration._

object Test  {
  def main(args: Array[String]) {
    val line = ("|" + (0 until 10).map(_.toString).mkString("") + "|\n").getBytes("UTF-8")

    val file = new File("/tmp/test.log")
    if (file.exists()) file.delete()

    val asyncOut = new AsyncBufferedOutputStream(
      new AsyncFileOutputStream(file), bufferSize = 1024 * 1024)

    var idx = 0

    while (idx < 10) {
      idx += 1

      val startedTS = System.currentTimeMillis()

      var count = 0
      while (count < 20000) {
        count += 1
        asyncOut.write(line)
      }

      val currentTS = System.currentTimeMillis()
      val startedSec = startedTS / 1000
      val currentSec = currentTS / 1000

      if (startedSec == currentSec) {
        this.synchronized(
          wait((currentSec + 1) * 1000 - startedTS))
        println(currentSec)
      }
    }

    asyncOut.tryClose().await(5.minutes)
  }
}
