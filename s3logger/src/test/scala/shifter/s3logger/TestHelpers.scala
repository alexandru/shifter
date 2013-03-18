package shifter.s3logger

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import collection.JavaConverters._
import concurrent.duration._
import java.io.{InputStreamReader, BufferedReader}
import util.Random
import java.util.zip.GZIPInputStream

trait TestHelpers {
  def startThread(logger: S3Logger, line: String, count: Int): Thread = {
    val thread = new Thread(new Runnable {
      def run() {
        (0 until count).foreach { x =>
          logger.write((line + "\n").getBytes("UTF-8"))
        }
      }
    })

    thread.run()
    thread
  }

  def startThreadWithCB(logger: S3Logger, line: String, count: Int)(cb: => Unit): Thread = {
    val thread = new Thread(new Runnable {
      def run() {
        (0 until count).foreach { x =>
          logger.write(line.getBytes("UTF-8"))
          cb
        }
      }
    })

    thread.run()
    thread
  }

  def withS3Client[T](cb: AmazonS3Client => T): T = {
    val env = System.getenv().asScala.flatMap {
      case (key, value) =>
        if (value == null || value.isEmpty)
          None
        else
          Some(key, value)
    }

    assert(env.get("GEKKO_AWS_ACCESSKEY").isDefined)
    assert(env.get("GEKKO_AWS_SECRETKEY").isDefined)

    val s3Credentials = new BasicAWSCredentials(env("GEKKO_AWS_ACCESSKEY"), env("GEKKO_AWS_SECRETKEY"))
    val client = new AmazonS3Client(s3Credentials)

    try {
      cb(client)
    }
    finally {
      client.shutdown()
    }
  }

  def deleteAllKeys() {
    withS3Client { s3client =>
      val (_, _, bucket) = getCredentials

      val list = s3client.listObjects(bucket).getObjectSummaries.asScala
        .map(_.getKey).filter(_.startsWith("test"))

      for (key <- list) {
        s3client.deleteObject(bucket, key)
      }
    }
  }

  def listKeys = {
    withS3Client { s3client =>
      val (_, _, bucket) = getCredentials

      s3client.listObjects(bucket).getObjectSummaries.asScala
        .map(_.getKey).filter(_.startsWith("test"))
    }
  }

  def withKey[T](key: String)(cb: BufferedReader => T) {
    withS3Client { s3client =>
      val (_, _, bucket) = getCredentials
      val request = s3client.getObject(bucket, key)
      val ins = new BufferedReader(
        new InputStreamReader(
          new GZIPInputStream(request.getObjectContent), "UTF-8"))

      try {
        cb(ins)
      }
      finally {
        ins.close()
      }
    }
  }

  def withSample[T](cb: (String, BufferedReader) => T) {
    withS3Client { s3client =>
      val (_, _, bucket) = getCredentials
      val list = s3client.listObjects(bucket).getObjectSummaries
        .asScala.map(_.getKey).filter(_.startsWith("test"))

      Random.shuffle(list).headOption match {
        case Some(key) =>
          val request = s3client.getObject(bucket, key)
          val ins = new BufferedReader(
            new InputStreamReader(
              new GZIPInputStream(request.getObjectContent), "UTF-8"))

          try {
            cb(key, ins)
          }
          finally {
            ins.close()
          }

        case None =>
          assert(assertion = false, "No sample available")
      }
    }
  }

  def withLogger[T](intervalSecs: Int)(cb: S3Logger => T): T = {
    val (access, secret, bucket) = getCredentials
    deleteAllKeys()

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

    try {
      cb(logger)
    }
    finally {
      deleteAllKeys()
    }
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

  def getBucketName =
    getCredentials._3
}
