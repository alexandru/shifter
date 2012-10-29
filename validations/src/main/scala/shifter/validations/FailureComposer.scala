package shifter.validations

import collection.{Map => IMap}


trait FailureComposer[-From, +To] {
  def compose(valA: Failure[From], valB: Failure[From]): Failure[To]
  def convert(value: Failure[From]): Failure[To]
}

object FailureComposer {
  implicit object ComposeStrings extends FailureComposer[String, String] {
    def compose(valA: Failure[String], valB: Failure[String]): Failure[String] = valA
    def convert(value: Failure[String]) = value
  }

  implicit object ComposeTraversables extends FailureComposer[Traversable[String], Seq[String]] {
    def compose(valA: Failure[Traversable[String]], valB: Failure[Traversable[String]]): Failure[Seq[String]] =
      Failure((valA.error ++ valB.error).toSeq.distinct)

    def convert(value: Failure[Traversable[String]]) =
      Failure(value.error.toSeq)
  }

  implicit object ComposeMaps extends FailureComposer[IMap[String, String], Map[String, String]] {
    def compose(valA: Failure[IMap[String, String]], valB: Failure[IMap[String, String]]) =
      Failure(valB.error.toMap ++ valA.error.toMap)

    def convert(value: Failure[IMap[String, String]]) =
      Failure(value.error.toMap)
  }
}


