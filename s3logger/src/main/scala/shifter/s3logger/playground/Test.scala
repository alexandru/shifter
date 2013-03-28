package shifter.s3logger.playground

import shifter.io.{AsyncGZIPOutputStream, AsyncBufferedOutputStream, AsyncFileOutputStream}
import java.io.File
import concurrent.duration._
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext.Implicits.global
import concurrent.Future

object Test  {
  def main(args: Array[String]) {

    val json = """
      |{
      |    "app":{
      |        "aid":"agltb3B1Yi1pbmNyDAsSA0FwcBirq7IWDA",
      |        "cat":[
      |            "entertainment",
      |            "photography"
      |        ],
      |        "global_aid":"org.faked.gag9",
      |        "name":"9gag",
      |        "paid":0,
      |        "pid":"agltb3B1Yi1pbmNyEAsSB0FjY291bnQYj77pEgw",
      |        "pub":"JMT Apps LLC"
      |    },
      |    "at":2,
      |    "device":{
      |        "carrier":"Wi-Fi",
      |        "city":"Augsburg",
      |        "country":"DEU",
      |        "dpid":"7d2f2918be3b0e81673d5625da451d5165258c88",
      |        "ip":"93.135.226.124",
      |        "js":1,
      |        "os":"Android",
      |        "osv":"4.1.2",
      |        "state":"02",
      |        "ua":"Mozilla/5.0 (Linux; U; Android 4.1.2; de-de; GT-I8190 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"
      |    },
      |    "id":"c6c528a0-97ba-4a37-ba5f-193224c04687",
      |    "imp":[
      |        {
      |            "battr":[
      |                "10",
      |                "14"
      |            ],
      |            "displaymanagerver":"1.9.0.5",
      |            "h":50,
      |            "impid":"c6c528a0-97ba-4a37-ba5f-193224c04687",
      |            "instl":0,
      |            "w":320
      |        }
      |    ],
      |    "pf":0.010,
      |    "restrictions":{
      |        "badv":[
      |
      |        ],
      |        "bcat":[
      |            "IAB25"
      |        ]
      |    },
      |    "tmax":300,
      |    "user":{
      |        "keywords":"sd:8",
      |        "uid":"7d2f2918be3b0e81673d5625da451d5165258c88"
      |    }
      |}
    """.stripMargin.replaceAll("\r?\n", " ") + "\n"

    val line = json.getBytes("UTF-8")

    val file = new File("/tmp/test.log.gz")
    if (file.exists()) file.delete()

    // TODO: BUGGY
    //val asyncOut = new AsyncBufferedOutputStream(
    //  new AsyncFileOutputStream(file), bufferSize = 30)
    val asyncOut =
        new AsyncBufferedOutputStream(
          new AsyncFileOutputStream(file)(global)
      )(global)

    //val testFile = new GZIPOutputStream(new FileOutputStream(new File("/tmp/test-diff.log.gz")))

    var idx = 0

    println("Starting ...")
    val iterationsRef = new AtomicInteger(0)

    while (idx < 100) {
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

      val iterations = iterationsRef.incrementAndGet()
      if (startedSec == currentSec) {
        println(iterations)
        //this.synchronized(
          //wait((currentSec + 1) * 1000 - startedTS))
      }
    }

    asyncOut.close(10.minutes)
    //testFile.close()
  }
}
