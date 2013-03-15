package shifter.s3logger.support

sealed trait HandlerState

case object NoHandler extends HandlerState
case object HandlerBorrowed extends HandlerState
case class HandlerAvailable(handler: Handler) extends HandlerState