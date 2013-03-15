import collection.JavaConverters._
import shifter.s3logger.{AWSConfiguration, Configuration, S3Logger}
import concurrent.duration._

object Test {
  def withLogger[T](intervalSecs: Int)(cb: S3Logger => T): T = {
    val (access, secret, bucket) = (
      "AKIAIZ6ZLBMBKB6IRH2A",
      "H5Fyfmt4Z6/9X25q/fOQzWwPJRHA0pzlp/sfjTVH",
      "epigrams-logs-test"
    )

    val config = Configuration(
      collection = "test",
      localDirectory = "/tmp",
      expiry = intervalSecs.seconds,
      maxSizeMB = 100,
      aws = Some(AWSConfiguration(
        accessKey = access,
        secretKey = secret,
        bucketName = bucket
      )),
      isEnabled = true
    )

    val logger = S3Logger(config)
    cb(logger)
  }

  def main(args: Array[String]) {
    withLogger(60 * 60) { s3logger =>
      for (i <- 1 to 10000)
        s3logger.write("hello.%d".format(i).getBytes("UTF-8"))

      val stats = s3logger.rotate(forced = false)
      println(stats)
    }
  }
}
