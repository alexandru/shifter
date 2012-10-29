package shifter.validations.playground

import collection.generic._
import collection.immutable._
  
                    
sealed abstract class Transformed[+E, +R] {
  def and[E2 >: E, R2 >: R](that: Transformed[E2, R2]): Transformed[E2, R2] =
    this match {
      case Success(values) => that match {
	  case Success(values2) =>
	    Success[E2, R2]((values ++ values2).distinct)
	  case Failure(_) =>
	    that
      }
      case Failure(errors) => that match {
	case Success(_) =>
	  this
	case Failure(errors2) =>
	  Failure[E2, R2]((errors ++ errors2).distinct)
      }
    }

  def or[E2 >: E, R2 >: R](that: Transformed[E2, R2]): Transformed[E2, R2] =
    this match {
      case Success(_) => that match {
	  case Success(_) =>
	    this and that
	  case Failure(_) =>
	    this
      }
      case Failure(_) => that match {
	case Success(_) =>
	  that
	case Failure(errors2) =>
	  this and that
      }
    }

  def values = List.empty[(String, R)]

  def errors = List.empty[(String, E)]

  def toMap = this match {
    case Success(values) => values.groupBy(_._1).map { case (k,v) => (k, v.map(_._2)) }
    case Failure(errors) => errors.groupBy(_._1).map { case (k,v) => (k, v.map(_._2)) }
  }
}

final case class Success[+E, +R](override val values: List[(String, R)]) extends Transformed[E, R]

object Success {
  def apply[T](key: String, value: T): Success[Nothing, T] =
    Success[Nothing, T](List(key -> value))
}

final case class Failure[+E, +R](override val errors: List[(String, E)]) extends Transformed[E, R]

object Failure {
  def apply[T](key: String, error: T): Failure[T, Nothing] =
    Failure[T, Nothing](List(key -> error))
}

trait Transformation[-T, +E, +R] extends Function2[String, T, Transformed[E, R]] {
  def andThen[T2 <: T, E2 >: E, R2 >: R](that: Transformation[R2, E2, R2]): Transformation[T2, E2, R2] = {
    val self = this
	
    new Transformation[T2, E2, R2] {
      def apply(key: String, value: T2): Transformed[E2, R2] = 
	self(key, value) match {
	  case Success((key2, value2) :: tail) =>
	    val first = that(key2, value2) 
	    if (tail.size > 0) {
	      val values = tail.map { case (k,v) => that(k,v) }
	      values.foldLeft(first)(_ and _)
	    }
	    else
	      first
	  case blank @ Success(Nil) =>
	    blank
	  case failure @ _ =>
	    failure
	}
    }
  }

  def and[T2 <: T, E2 >: E, R2 >: R](that: Transformation[T2, E2, R2]): Transformation[T2, E2, R2] = {
    val self = this

    new Transformation[T2, E2, R2] {
      def apply(key: String, value: T2): Transformed[E2, R2] = 
	self(key, value) and that(key, value)
    }
  }

  def or[T2 <: T, E2 >: E, R2 >: R](that: Transformation[T2, E2, R2]): Transformation[T2, E2, R2] = {
    val self = this

    new Transformation[T2, E2, R2] {
      def apply(key: String, value: T2): Transformed[E2, R2] = 
	self(key, value) or that(key, value)
    }
  }
}

class Integer(msg: String = "not a valid integer") extends Transformation[String, String, Int] {
  def apply(key: String, value: String): Transformed[String, Int] = 
    value match {
      case NumberFormat(x) => Success(key, x.toInt)
      case _ => Failure(key, msg)
    }
    
  private[this] val NumberFormat = """^(\d+)$""".r
}

object int extends Integer {
  def apply(msg: String) = new Integer(msg)
}

class IntegerMinusOne(msg: String = "not a valid integer") extends Transformation[String, String, Int] {
  def apply(key: String, value: String): Transformed[String, Int] = 
    value match {
      case NumberFormat(x) => Success(key, x.toInt - 1)
      case _ => Failure(key, msg)
    }
    
  private[this] val NumberFormat = """^(\d+)$""".r
}

object intMinusOne extends Integer {
  def apply(msg: String) = new IntegerMinusOne(msg)
}

class WithinRange(min: Int, max: Int, msg: String = "must be within range") extends Transformation[Int, String, Int] {
  def apply(key: String, value: Int): Transformed[String, Int] = 
    if (value >= min && value <= max)
      Success(key, value)
    else
      Failure(key, msg)
}

object withinRange {
  def apply(min: Int, max: Int, msg: String = "must be within range") = new WithinRange(min, max, msg)
}

