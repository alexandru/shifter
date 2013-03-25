package shifter.s3logger.parallel

import shifter.s3logger.S3Logger
import java.util.concurrent.atomic.AtomicInteger
import shifter.s3logger.support.{Helpers, UploadedFileInfo}
import util.Try
import com.amazonaws.{AmazonServiceException, AmazonClientException}

class S3ParallelLogger(config: Configuration) extends S3Logger with Helpers {
  require(config.parallelism > 0, "config.parallelism > 0")

  def write(content: String) {
    val counter = counterRef.incrementAndGet()
    val pickedIdx = counter % parallelism
    val pickedHandle = handles(pickedIdx)
    pickedHandle.write(content)
  }

  def rotate(forced: Boolean): Seq[UploadedFileInfo] =
    rotateLock.synchronized {
      for (handle <- handles) {
        val shouldRotate = handle.hasWrites && (
            forced ||
            handle.getFileSizeMB >= config.maxSizeMB ||
            handle.lifespanInMillis > config.expiry.toMillis
          )

        if (shouldRotate)
          handle.rotate(getFile("for-upload"))
      }

      val forUpload = localDirectoryFile.listFiles().filter { file =>
        file.getName.startsWith(config.collection + "--slash--") &&
          file.getName.endsWith(".for-upload.log.gz")
      }

      val stats = forUpload.foldLeft(Vector.empty[Option[UploadedFileInfo]]) { (acc, file) =>
        val infoItem = try {
          val s3Key = s3KeyFromFile(file)
          s3Client.putObject(config.aws.bucketName, s3Key, file)
          val info = UploadedFileInfo(
            s3Key = s3Key,
            s3Bucket = config.aws.bucketName,
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


  protected val awsAccessKey: String = config.aws.accessKey
  protected val awsSecretKey: String = config.aws.secretKey

  protected val collection: String = config.collection
  protected val localDirectory: String = config.localDirectory

  private[this] val rotateLock = new AnyRef
  private[this] val parallelism = config.parallelism
  private[this] val counterRef = new AtomicInteger(-1)
  private[this] val handles = Vector.fill(config.parallelism) {
    new FileHandle(
      prefix = config.collection + ".",
      suffix = ".log.gz",
      localDir = Some(config.localDirectory)
    )
  }
}
