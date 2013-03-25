package shifter.s3logger.support

import java.io.File
import java.util.{UUID, Calendar}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.auth.BasicAWSCredentials

trait Helpers {
  protected val collection: String
  protected val localDirectory: String
  protected val awsAccessKey: String
  protected val awsSecretKey: String

  protected val secret = "kjYhn9UthKlP9HnLuYt"

  protected def s3KeyFromFile(file: File): String = {
    val S3Key = """(^\w+/dt=\d{4}-\d{2}-\d{2}/\d{14}-\w+).*[.]log\.gz""".r
    file.getName.replace("--slash--", "/") match {
      case S3Key(str) => str + ".log.gz"
      case _ => generateS3KeyPrefix + ".log.gz"
    }
  }

  protected def getFile(label: String): File = {
    val s3Key = generateS3KeyPrefix
    val prefix = s3Key.replace("/", "--slash--")
    new File(localDirectoryFile, String.format("%s.%s.%s.log.gz", prefix, secret, label))
  }

  protected def generateS3KeyPrefix: String = {
    val now = Calendar.getInstance()

    "%s/dt=%d-%02d-%02d/%d%02d%02d%02d%02d%02d-%s".format(
      collection,
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

  protected lazy val localDirectoryFile = {
    val localDir =
      if (!localDirectory.isEmpty) {
        val dir = new File(localDirectory)
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

  protected lazy val s3Client: AmazonS3Client = {
    val s3Credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey)
    new AmazonS3Client(s3Credentials)
  }
}
