package shifter.s3logger.support

import java.util.concurrent.atomic.AtomicReference
import java.io._
import java.util.zip.GZIPOutputStream
import java.util.{UUID, Calendar}
import annotation.tailrec
import util.Try
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.{AmazonServiceException, AmazonClientException}
import shifter.s3logger.S3Logger
import shifter.s3logger.Configuration


/**
 * Thread safe, non-blocking implementation of an S3Logger.
 */
class S3ActiveLogger(config: Configuration) extends S3Logger {

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
      case NoHandler =>
        Seq.empty

      case HandlerBorrowed =>
        // handler is busy, retry until it succeeds
        doRotate(forced)

      case current@HandlerAvailable(handler) =>
        val shouldRotate =
          handler.writes > 0 && (
            forced ||
            getElapsed(handler) >= rotateMillis ||
            getFileSize(handler) >= maxSizeBytes
          )

        if (!shouldRotate)
          Seq.empty

        // pull out handler for rotation
        else if (!handlerRef.compareAndSet(current, NoHandler))
          doRotate(forced)

        // we've got a handler that we can rotate
        else
          // locking here, although it shouldn't be necessary, just in case
          rotateLock.synchronized {
            // close output stream
            handler.out.close()
            // rename file
            val renamed = getFile("for-upload")
            handler.file.renameTo(renamed)

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
        case current@HandlerAvailable(ref) =>
          // borrow handler for writing
          if (handlerRef.compareAndSet(current, HandlerBorrowed))
            ref
          else
            // state changed, retry
            fetchHandler

        case HandlerBorrowed =>
          // handler is busy, retry again until it succeeds
          fetchHandler

        case NoHandler =>
          val file = File.createTempFile(config.collection, ".log.gz", localDirectory)
          file.deleteOnExit()

          val fileStream = new FileOutputStream(file)
          val out =
              new GZIPOutputStream(new BufferedOutputStream(fileStream))
          val handler = Handler(file, fileStream, out, System.currentTimeMillis())

          if (!handlerRef.compareAndSet(NoHandler, HandlerBorrowed)) {
            // we couldn't create handler, revert everything and try again
            Try(out.close())
            Try(fileStream.close())
            Try(file.delete())
            fetchHandler
          }
          else
            handler
      }

    val handler = fetchHandler

    try {
      cb(handler)
    }
    finally {
      val returnHandler = handler.copy(writes = handler.writes + 1)
      if (!handlerRef.compareAndSet(HandlerBorrowed, HandlerAvailable(returnHandler)))
        throw new IllegalStateException(
          "S3Logger's Handler is in state %s and should have been in HandlerBorrowed state"
            .format(handlerRef.get().getClass.getName))
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
  private[this] def getFileSize(handler: Handler) =
    handler.fileStream.getChannel.size()

  @inline
  private[this] def getElapsed(handler: Handler) =
    System.currentTimeMillis() - handler.createdTS

  private[this] val handlerRef =
    new AtomicReference(NoHandler : HandlerState)

  private[this] val aws = config.aws match {
    case Some(value) => value
    case None => throw S3LoggerException("Config for AWS missing")
  }
  private[this] val s3Client: AmazonS3Client = {
    val s3Credentials = new BasicAWSCredentials(aws.accessKey, aws.secretKey)
    new AmazonS3Client(s3Credentials)
  }

  private[this] val rotateLock = new AnyRef
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
