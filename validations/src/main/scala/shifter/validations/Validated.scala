package shifter.validations

sealed abstract class Validated[+E] {
  def and[F >: E, That](that: Validated[F])(implicit cf: FailureComposer[F, That]): Validated[That] =
    that match {
      case Success => this match {
	case Success => Success
	case failedThis @ Failure(_) =>
	  cf.convert(failedThis)
      }
      case failedThat @ Failure(_) => this match {
	case Success => cf.convert(failedThat)
	case failedThis @ Failure(_) =>
	  cf.compose(failedThis, failedThat)
      }
    }

  def or[F >: E](that: Validated[F]) = this match {
    case Success => this
    case Failure(_) => that 
  }

  def onFailure[F >: E](f: F => Unit): Validated[E] = this
  def onSuccess(f: => Unit): Validated[E] = this

  final def & [F >: E, That](that: Validated[F])(implicit cf: FailureComposer[F, That]) = and(that)(cf)
  final def | [F >: E](that: Validated[F]) = or(that)
}

case class Failure[+E](error: E) extends Validated[E] {
  override def onFailure[F >: E](f: F => Unit) = 
    { f(error); this }
}

case object Success extends Validated[Nothing] {
  override def onSuccess(f: => Unit) = 
    { f; this }
}


object Validated {
  import FailureComposer._
}
