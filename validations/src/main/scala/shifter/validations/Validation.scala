package shifter.validations


trait Validation[-A, +E] extends Function1[A, Validated[E]] {
  def apply(value: A): Validated[E]

  def and[B <: A, F >: E, That](that: Validation[B, F])(implicit cf: FailureComposer[F, That]): Validation[B, That] = {
    val self = this
    new Validation[B, That] {
      def apply(value: B) = 
	self(value).and(that(value))(cf)
    }
  }

  def or[B <: A, F >: E](that: Validation[B, F]) = {
    val self = this
    new Validation[B, F] {
      def apply(value: B) = 
	self(value) or that(value)
    }
  }

  final def & [B <: A, F >: E, That](that: Validation[B, F])(implicit cf: FailureComposer[F, That]) = and(that)(cf)
  final def | [B <: A, F >: E](that: Validation[B, F]) = or(that)
}


object Validation {
  import FailureComposer._

  def apply[A, E](f: A => Validated[E]) = 
    new Validation[A, E] {
      def apply(value: A): Validated[E] = f(value)
    }
}
