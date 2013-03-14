package shifter.s3logger

import java.io._
import scala.Some
import util.Try
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.auth.BasicAWSCredentials
import java.util.{UUID, Calendar}
import java.util.zip.GZIPOutputStream
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}
import collection.immutable.Queue
import annotation.tailrec


class S3ActiveLogger private[s3logger] (config: Configuration) extends S3Logger {
  def write(content: Array[Byte]) {
    // inspect queue content
    val (ts, _) = queueRef.get()
    val currentTS = System.currentTimeMillis()

    if (ts > 0 && currentTS > ts + config.syncMillis)
      flushQueueContent()
    writeToQueue(content)
  }

  @tailrec
  private[this] def writeToQueue(content: Array[Byte]) {
    // push content in our queue
    val current = queueRef.get()
    val currentTS = System.currentTimeMillis()
    val (_, queue) = current

    val newQueue = queue.enqueue(content)
    if (!queueRef.compareAndSet(current, (currentTS, newQueue)))
      writeToQueue(content)
  }

  @tailrec
  private[this] def flushQueueContent() {
    val item = queueRef.get()

    // non blocking
    if (!queueRef.compareAndSet(item, (0, Queue.empty)))
      flushQueueContent()

    else
      withHandler { out =>
        val queue = item._2

        val addedSize =
          queue.foldLeft(0L) { (processed, line) =>
            out.write(line)
            processed + line.length
          }

        out.flush()

        assert(stats.isDefined)
        stats = stats.map(obj => obj.copy(
          sizeInBytes = obj.sizeInBytes + addedSize
        ))
      }
  }

  def rotate(forced: Boolean): Boolean =
    rotateLock.synchronized {
      // uploading previously failed instances
      compressFiles()
      uploadToS3()
      flushQueueContent()

      val shouldRotate = forced || isReadyForUpload

      if (shouldRotate && rotateCurrent()) {
        compressFiles()
        uploadToS3()
      }
      else
        false
    }

  private[this] def isReadyForUpload: Boolean =
    writeLock.synchronized {
      stats match {
        case None =>
          false
        case Some(obj) =>
          val currentSizeInMB = obj.sizeInBytes / (1024 * 1024)
          val currentTS = System.currentTimeMillis()
          val interval = config.expiry.toMillis
          val isExpired = currentTS - createdTS.get() >= interval

          (isExpired && obj.sizeInBytes > 0) || currentSizeInMB >= config.maxSizeMB
      }
    }

  private[this] def uploadToS3(): Boolean =
    rotateLock.synchronized {
      if (isS3Available) {
        val forUpload = listForUpload

        for (path <- forUpload) {
          val file = new File(path)
          assert(file.exists())

          val s3Key = file.getName.replace(".for-upload.log.gz", ".log.gz").replace("--slash--", "/")
            .replace("." + secret, "")

          s3Client.putObject(aws.bucketName, s3Key, file)
          file.delete()
        }

        forUpload.length > 0
      }
      else
        false
    }

  private[this] def compressFiles(): Boolean =
    rotateLock.synchronized {
      val forCompress = listForCompress
      var processed = List.empty[String]

      try {
        for (path <- forCompress) {
          val destination = getFile("for-upload.log.gz")
          assert(!destination.exists())
          val src = new File(path)
          assert(src.exists())

          if (src.length() > 0) {
            val out = new GZIPOutputStream(new FileOutputStream(destination))
            val in = new BufferedInputStream(new FileInputStream(src))

            try {
              val buffer = Array.fill(1024 * 4)(0.toByte)
              var bytesRead = -1

              do {
                bytesRead = in.read(buffer)
                if (bytesRead > 0) {
                  out.write(buffer, 0, bytesRead)
                }
              } while (bytesRead > -1)

              processed = destination.getAbsolutePath :: processed
            }
            finally {
              out.close()
              in.close()
            }
          }
        }

        forCompress.foreach(path => Try(new File(path).delete()))
      }
      catch {
        case ex: Exception =>
          // reverting everything in case stuff broke
          processed.map(path => new File(path)).filter(_.exists()).foreach(_.delete())
          throw ex
      }

      processed.length > 0
    }

  private[this] def rotateCurrent(): Boolean =
    rotateLock.synchronized {
      writeLock.synchronized {
        if (handler.isDefined) {
          handler.get.close()
          createdTS.set(System.currentTimeMillis())

          handler = None
          stats = None
          val currentLogs = listCurrent

          for (currentPath <- currentLogs) {
            val destination = getFile("for-compress.log")

            assert(!destination.exists(),
              "Cannot rotate because destination file for compression already exists!")

            val current = new File(currentPath)
            current.renameTo(destination)
          }

          currentLogs.length > 0
        }
        else
          false
      }
    }

  private[this] def withHandler[T](cb: OutputStream => T): T =
    writeLock.synchronized {
      val outHandler = handler match {
        case None =>
          // opens new handler
          val file = getFile("current.log")
          assert(!file.exists(), "current.log already exists, something went wrong")

          val out = new BufferedOutputStream(
            new FileOutputStream(file)
          )
          handler = Some(out)
          stats = Some(CollectionStats(
            path = file.getAbsolutePath,
            sizeInBytes = 0
          ))

          out
        case Some(obj) => obj
      }
      cb(outHandler)
    }

  private[this] def getFile(suffix: String): File = {
    val now = Calendar.getInstance()

    val logTS = "%s--slash--dt=%d-%02d-%02d--slash--%d%02d%02d%02d%02d%02d-%s".format(
      config.collection,
      now.get(Calendar.YEAR),
      now.get(Calendar.MONTH) + 1,
      now.get(Calendar.DAY_OF_MONTH),
      now.get(Calendar.YEAR),
      now.get(Calendar.MONTH) + 1,
      now.get(Calendar.DAY_OF_MONTH),
      now.get(Calendar.HOUR_OF_DAY),
      now.get(Calendar.MINUTE),
      now.get(Calendar.SECOND),
      UUID.randomUUID().toString.split("-")(0)
    )

    val localDir = new File(config.localDirectory)
    new File(localDir, String.format("%s.%s.%s", logTS, secret, suffix))
  }

  private[this] def listCurrent: Seq[String] = {
    val localDir = new File(config.localDirectory)
    localDir.listFiles().toSeq.map(_.getAbsolutePath).filter { path =>
      path.endsWith(".current.log")
    }
  }

  private[this] def listForCompress: Seq[String] = {
    val localDir = new File(config.localDirectory)
    localDir.listFiles().toSeq.map(_.getAbsolutePath).filter { path =>
      path.endsWith(".for-compress.log")
    }
  }

  private[this] def listForUpload: Seq[String] = {
    val localDir = new File(config.localDirectory)
    localDir.listFiles().toSeq.map(_.getAbsolutePath).filter { path =>
      path.endsWith(".for-upload.log.gz")
    }
  }

  private[this] def isS3Available: Boolean = {
    val file = File.createTempFile("test", "txt")
    file.deleteOnExit()

    val out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")
    out.write("test")
    out.close()

    val key =  "test/" + file.getName

    try {
      s3Client.putObject(aws.bucketName, key, file)
      true
    } catch {
      case _: Exception => false
    } finally {
      file.delete()
      Try(s3Client.deleteObject(aws.bucketName, key))
    }
  }

  private[this] val secret = "jMhtKmL89YhjlLhnJp7812J98GbvD"

  // these 2 mutable variables must always be protected by the writeLock
  private[this] var handler: Option[OutputStream] = None
  private[this] var stats: Option[CollectionStats] = None
  private[this] val createdTS = new AtomicLong(System.currentTimeMillis())

  private[this] val queueRef = new AtomicReference(
    (0L, Queue.empty[Array[Byte]])
  )

  private[this] val rotateLock = new AnyRef
  private[this] val writeLock = new AnyRef

  private[this] val aws = config.aws.get
  private[this] val s3Client: AmazonS3Client = {
    val s3Credentials = new BasicAWSCredentials(aws.accessKey, aws.secretKey)
    new AmazonS3Client(s3Credentials)
  }
}

