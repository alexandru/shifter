package shifter.lang.backports


/**
 * Backport of Try from Scala 2.10
 * http://www.scala-lang.org/api/milestone/index.html#scala.util.Try
 */
sealed abstract class Try[+T] {
  def failed: Try[Throwable]
  
  def filter(p: T => Boolean): Try[T]

  def flatMap[U](f: T => Try[U]): Try[U]

  def flatten[U](implicit ev: <:<[T, Try[U]]): Try[U]

  def foreach[U](f: T => U): Unit

  def get: T

  def isFailure: Boolean

  def isSuccess: Boolean
  
  def map[U](f: T => U): Try[U]

  def recover[U >: T](f: PartialFunction[Throwable, U]): Try[U]

  def recoverWith[U >: T](f: PartialFunction[Throwable, Try[U]]): Try[U]

  def getOrElse[U >: T](default: => U): U =
    if (isSuccess) get
    else default

  def orElse[U >: T](default: => Try[U]): Try[U] = 
    if (isSuccess) this
    else default

  def toOption: Option[T] = this match {
    case Success(x) => Some(x)
    case Failure(_) => None
  }

  def transform[U](s: T => Try[U], f: Throwable => Try[U]): Try[U] = this match {
    case Success(v) => s(v)
    case Failure(e) => f(e)
  }
}


object Try {
  def apply[T](r: => T): Try[T] = {
    try { Success(r) } catch {
      case NonFatal(e) => Failure(e)
    }
  }
}


final case class Success[+T](value: T) extends Try[T] {
  def isFailure: Boolean = false

  def isSuccess: Boolean = true

  def recoverWith[U >: T](f: PartialFunction[Throwable, Try[U]]): Try[U] = Success(value)

  def get = value

  def flatMap[U](f: T => Try[U]): Try[U] =
    try f(value)
    catch {
      case NonFatal(e) => Failure(e)
    }

  def flatten[U](implicit ev: T <:< Try[U]): Try[U] = value

  def foreach[U](f: T => U): Unit = f(value)

  def map[U](f: T => U): Try[U] = Try[U](f(value))

  def filter(p: T => Boolean): Try[T] = {
    try {
      if (p(value)) this
      else Failure(new NoSuchElementException("Predicate does not hold for " + value))
    } catch {
      case NonFatal(e) => Failure(e)
    }
  }

  def recover[U >: T](rescueException: PartialFunction[Throwable, U]): Try[U] = this

  def failed: Try[Throwable] = Failure(new UnsupportedOperationException("Success.failed"))
}


final case class Failure[+T](val exception: Throwable) extends Try[T] {
  def isFailure: Boolean = true

  def isSuccess: Boolean = false

  def recoverWith[U >: T](f: PartialFunction[Throwable, Try[U]]): Try[U] =
    if (f.isDefinedAt(exception)) f(exception) else this

  def get: T = throw exception

  def flatMap[U](f: T => Try[U]): Try[U] = Failure[U](exception)

  def flatten[U](implicit ev: T <:< Try[U]): Try[U] = Failure[U](exception)

  def foreach[U](f: T => U): Unit = {}

  def map[U](f: T => U): Try[U] = Failure[U](exception)

  def filter(p: T => Boolean): Try[T] = this

  def recover[U >: T](rescueException: PartialFunction[Throwable, U]): Try[U] = {
    try {
      if (rescueException.isDefinedAt(exception)) {
        Try(rescueException(exception))
      } else {
        this
      }
    } catch {
      case NonFatal(e) => Failure(e)
    }
  }

  def failed: Try[Throwable] = Success(exception)
}
