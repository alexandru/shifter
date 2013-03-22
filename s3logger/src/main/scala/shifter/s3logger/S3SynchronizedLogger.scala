package shifter.s3logger

import java.util.concurrent.atomic.AtomicReference
import java.io._
import java.util.zip.GZIPOutputStream
import java.util.{UUID, Calendar}
import annotation.tailrec
import support.{S3LoggerException, Handler, UploadedFileInfo}
import util.Try
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.{AmazonServiceException, AmazonClientException}
import java.util.concurrent.locks.ReentrantReadWriteLock


/**
 * Thread safe, non-blocking implementation of an S3Logger.
 */
class S3SynchronizedLogger(config: Configuration) extends S3Logger {

  def write(content: Array[Byte]) {
    withWriteHandler { handler =>
      handler.out.write(content)
      true
    }
  }

  def rotate(forced: Boolean): Seq[UploadedFileInfo] =
    doRotate(forced)

  @tailrec
  private[this] final def doRotate(forced: Boolean): Seq[UploadedFileInfo] =
    handlerRef.get() match {
      case None =>
        Seq.empty

      case current @ Some(handler) =>
        val fileSize = getFileSize(handler)
        val shouldRotate =
          forced ||
          getElapsed(handler) >= rotateMillis ||
          fileSize >= maxSizeBytes

        if (!shouldRotate)
          Seq.empty

        // pull out handler for rotation
        else if (!handlerRef.compareAndSet(current, None))
          doRotate(forced)

        // we've got a handler that we can rotate
        else
          // also acquire the rotateLock to prevent other concurrent
          // rotates and to be able to release the handlerLock quickly
          rotateLock.synchronized {
            handlerLock.writeLock().lock()

            // rename file quickly, then release the lock
            try {
              // close output stream
              handler.out.close()
              // rename file
              val renamed = getFile("for-upload")
              handler.file.renameTo(renamed)
            }
            finally {
              handlerLock.writeLock().unlock()
            }

            // upload all files that are prefixed with "for-upload.log.gz"
            val forUpload = localDirectory.listFiles().filter { file =>
              file.getName.startsWith(config.collection + "--slash--") &&
                file.getName.endsWith(".for-upload.log.gz")
            }

            val stats = forUpload.foldLeft(Vector.empty[Option[UploadedFileInfo]]) { (acc, file) =>
              val infoItem = try {
                val s3Key = s3KeyFromFile(file)
                s3Client.putObject(aws.bucketName, s3Key, file)
                val info = UploadedFileInfo(
                  s3Key = s3Key,
                  s3Bucket = aws.bucketName,
                  collection = config.collection,
                  fileSizeInBytes = file.length(),
                  uploadedTS = System.currentTimeMillis()
                )

                Try(file.delete())
                Some(info)
              }
              catch {
                // in case communication with Amazon fails, the file is not deleted,
                // and no exception gets reported, such that we may try at a later time
                case ex: AmazonClientException => None
                case ex: AmazonServiceException => None
              }

              acc :+ infoItem
            }

            stats.flatten
          }
    }

  private[this] def withWriteHandler(cb: Handler => Boolean): Boolean = {
    @tailrec
    def fetchHandler: Handler =
      handlerRef.get() match {
        case Some(ref) =>
          ref

        case None =>
          handlerLock.readLock().unlock()
          handlerLock.writeLock().lock()

          // can have a race condition here
          if (handlerRef.get().isDefined) {
            handlerLock.writeLock().unlock()
            handlerLock.readLock().lock()
            // recursive retry
            fetchHandler
          }
          else
            try {
              val file = File.createTempFile(config.collection, ".log.gz", localDirectory)
              file.deleteOnExit()

              val fileStream = new FileOutputStream(file)
              val out =
                new BufferedOutputStream(new GZIPOutputStream(fileStream))
              val handler = Handler(file, fileStream, out, System.currentTimeMillis())

              handlerRef.set(Some(handler))
              handler
            }
            finally {
              // restore read lock state
              handlerLock.writeLock().unlock()
              handlerLock.readLock().lock()
            }
      }

    handlerLock.readLock().lock()
    try {
      val handler = fetchHandler
      cb(handler)
    }
    finally {
      handlerLock.readLock().unlock()
    }
  }

  private[this] def s3KeyFromFile(file: File): String = {
    val S3Key = """(^\w+/dt=\d{4}-\d{2}-\d{2}/\d{14}-\w+).*[.]log\.gz""".r
    file.getName.replace("--slash--", "/") match {
      case S3Key(str) => str + ".log.gz"
      case _ => generateS3KeyPrefix + ".log.gz"
    }
  }

  private[this] def getFile(label: String): File = {
    val s3Key = generateS3KeyPrefix
    val prefix = s3Key.replace("/", "--slash--")
    new File(localDirectory, String.format("%s.%s.%s.log.gz", prefix, secret, label))
  }

  private[this] def generateS3KeyPrefix: String = {
    val now = Calendar.getInstance()

    "%s/dt=%d-%02d-%02d/%d%02d%02d%02d%02d%02d-%s".format(
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
  }

  @inline
  private[this] def getFileSize(handler: Handler) = {
    handler.out.flush()
    handler.fileStream.getChannel.size()
  }

  @inline
  private[this] def getElapsed(handler: Handler) =
    System.currentTimeMillis() - handler.createdTS

  private[this] val handlerRef =
    new AtomicReference(None : Option[Handler])

  private[this] val aws = config.aws match {
    case Some(value) => value
    case None => throw S3LoggerException("Config for AWS missing")
  }
  private[this] val s3Client: AmazonS3Client = {
    val s3Credentials = new BasicAWSCredentials(aws.accessKey, aws.secretKey)
    new AmazonS3Client(s3Credentials)
  }

  private[this] val rotateLock = new AnyRef
  private[this] val handlerLock = new ReentrantReadWriteLock(false)

  private[this] val secret = "kjYhn9UthKlP9HnLuYt"
  private[this] val rotateMillis = config.expiry.toMillis
  private[this] val maxSizeBytes = config.maxSizeMB * 1024 * 1024

  private[this] val localDirectory = {
    val localDir =
      if (!config.localDirectory.isEmpty) {
        val dir = new File(config.localDirectory)
        if (dir.exists() && dir.isDirectory && dir.canWrite)
          Some(dir)
        else
          None
      }
      else
        None

    localDir match {
      case Some(dir) => dir
      case None =>
        val tmpDir = Option(System.getProperty("java.io.tmpdir"))
        if (tmpDir.isDefined && !tmpDir.get.isEmpty)
          new File(tmpDir.get)
        else
          new File("/tmp")
    }
  }
}
