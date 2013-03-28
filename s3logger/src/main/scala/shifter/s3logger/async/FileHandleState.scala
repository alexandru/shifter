package shifter.s3logger.async

import java.io.File
import shifter.io.AsyncOutputStream


sealed trait FileHandleState
case object NotInitialized extends FileHandleState
case class Available(
  startedAtTs: Long,
  file: File,
  out: AsyncOutputStream
) extends FileHandleState