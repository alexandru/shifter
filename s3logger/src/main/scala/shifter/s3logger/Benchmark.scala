package shifter.s3logger

import collection.JavaConverters._
import concurrent.duration._
import java.util.concurrent.{TimeUnit, Executors}


object Benchmark extends App {
  val json =
    """
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
    """.stripMargin.replaceAll("\r?\n", " ")

  val pool = Executors.newCachedThreadPool()

  def startThread(logger: S3Logger, line: String, count: Int) =
    pool.submit(new Runnable {
      def run() {
        var idx = 0
        while (idx < count) {
          idx += 1
          logger.write((line + "\n").getBytes("UTF-8"))
        }
      }
    })

  val env = System.getenv().asScala.flatMap {
    case (key, value) =>
      if (value == null || value.isEmpty)
        None
      else
        Some(key, value)
  }

  def getCredentials = {
    val env = System.getenv().asScala.flatMap {
      case (key, value) =>
        if (value == null || value.isEmpty)
          None
        else
          Some(key, value)
    }

    assert(env.get("GEKKO_AWS_ACCESSKEY").isDefined)
    assert(env.get("GEKKO_AWS_SECRETKEY").isDefined)
    assert(env.get("GEKKO_AWS_BUCKETNAME").isDefined)

    (env("GEKKO_AWS_ACCESSKEY"), env("GEKKO_AWS_SECRETKEY"), env("GEKKO_AWS_BUCKETNAME"))
  }

  assert(env.get("GEKKO_AWS_ACCESSKEY").isDefined)
  assert(env.get("GEKKO_AWS_SECRETKEY").isDefined)

  val (access, secret, bucket) = getCredentials

  val config = Configuration(
    collection = "test",
    localDirectory = "/tmp",
    expiry = 1.minute,
    maxSizeMB = 100,
    aws = Some(AWSConfiguration(
      accessKey = access,
      secretKey = secret,
      bucketName = bucket
    )),
    isEnabled = true
  )

  val logger = new S3SynchronizedLogger(config)

  pool.submit(new Runnable {
    def run() {
      while (true) {
        println("Rotate: " + logger.rotate(forced=false).toString)
        synchronized {
          wait(1000 * 20)
        }
      }
    }
  })

  // warmup
  (0 until 10).map(nr =>
    startThread(logger, json, 10000))
    .toList
    .foreach(_.get())

  println("STARTING")
  val startTs = System.currentTimeMillis()
  val threads = (0 until 10).map(nr =>
    startThread(logger, json, 30000))
    .toList

  threads.foreach(th => th.get())
  logger.rotate(forced = true)

  println("TIME: %d".format(System.currentTimeMillis()- startTs))
  println("Finished, press any key to exit...")
  Console.readLine()
}
