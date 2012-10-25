package shifter.validations


trait Validation[-A, +E] extends Function1[A, Validated[A, E]] {
  def apply(value: A): Validated[A, E]

  def and[B <: A, F >: E](that: Validation[B, F]) = {
    val self = this
    new Validation[B, F] {
      def apply(value: B) = 
	self(value) and that(value)
    }
  }

  def or[B <: A, F >: E](that: Validation[B, F]) = {
    val self = this
    new Validation[B, F] {
      def apply(value: B) = 
	self(value) or that(value)
    }
  }
}

