package shifter.s3logger.concurrent

sealed trait HandleState

case object NoHandle extends HandleState
case object HandleBorrowed extends HandleState
case class HandleAvailable(handle: Handle) extends HandleState