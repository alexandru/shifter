package shifter.s3logger.support

case class UploadedFileInfo(
  s3Key: String,
  s3Bucket: String,
  collection: String,
  fileSizeInBytes: Long,
  uploadedTS: Long
)