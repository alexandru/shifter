package shifter.s3logger.async

sealed trait FileHandleState
case object NotInitialized extends FileHandleState
case object Borrowed extends FileHandleState
case object Available extends FileHandleState