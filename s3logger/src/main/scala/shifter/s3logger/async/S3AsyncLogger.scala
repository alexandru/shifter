package shifter.s3logger.async

import shifter.s3logger.S3Logger
import java.io.File
import concurrent.ExecutionContext
import shifter.s3logger.support.UploadedFileInfo
import java.util.{UUID, Calendar}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.auth.BasicAWSCredentials
import util.Try
import com.amazonaws.{AmazonServiceException, AmazonClientException}


class S3AsyncLogger(config: Configuration)(implicit ec: ExecutionContext) extends S3Logger {
  def write(content: String) {
    fileHandle.write(content)
  }

  def rotate(forced: Boolean): Seq[UploadedFileInfo] = {
    val currentSizeMB = fileHandle.getFileSizeMB
    val shouldRotate =
      forced ||
      fileHandle.lifespanInMillis >= rotateMillis ||
      currentSizeMB >= config.maxSizeMB

    if (shouldRotate && fileHandle.rotate(getFile("for-upload"))) {
      val forUpload = localDirectory.listFiles().filter { file =>
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
    else
      Seq.empty
  }

  private[this] val s3Client: AmazonS3Client = {
    val s3Credentials = new BasicAWSCredentials(config.aws.accessKey, config.aws.secretKey)
    new AmazonS3Client(s3Credentials)
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
    new File(config.localDirectory, String.format("%s.%s.%s.log.gz", prefix, secret, label))
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

  private[this] val localDirectory = new File(config.localDirectory)
  private[this] val rotateMillis = config.expiry.toMillis
  private[this] val secret = "90as9fhuaiwekfa"
  private[this] val fileHandle = new FileHandle(config.collection + "-", ".log.gz", Some(config.localDirectory))
}
