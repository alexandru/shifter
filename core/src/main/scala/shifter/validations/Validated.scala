package shifter.validations


sealed abstract class Validated[-A, +E] {
  def and[B <: A, F >: E](that: Validated[B, F]) = that match {
    case Success()  => this
    case Failure(_) => this match {
      case Success() => that
      case Failure(_) => this
    }
  }

  def or[B <: A, F >: E](that: Validated[B, F]) = this match {
    case Success() => this
    case Failure(_) => that 
  }

  def onFailure[F >: E](f: F => Unit): Validated[A, E]
  def onSuccess[B <: A](f: => Unit): Validated[A, E]
}

case class Success[-T, +E]() extends Validated[T, E] {
  def onFailure[F >: E](f: F => Unit) = this
  def onSuccess[B <: T](f: => Unit) = { f; this }
}

case class Failure[-T, +E](error: E) extends Validated[T, E] {
  def onFailure[F >: E](f: F => Unit) = { f(error); this }
  def onSuccess[B <: T](f: => Unit) = this
}

