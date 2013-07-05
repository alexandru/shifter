package shifter.json.sax

import org.apache.commons.lang.StringEscapeUtils

sealed trait Event {
  override lazy val toString = this match {
    case Event.Error(path, value) =>
      "Event.Error(" + path.toString + ", \"" + StringEscapeUtils.escapeJava(value) + "\")"
    case Event.String(path, value) =>
      "Event.String(" + path.toString + ", \"" + StringEscapeUtils.escapeJava(value) + "\")"
    case Event.Long(path, value) =>
      "Event.Long(" + path.toString + ", " + value.toString + ")"
    case Event.Bool(path, value) =>
      "Event.Bool(" + path.toString + ", " + value.toString + ")"
    case Event.Double(path, value) =>
      "Event.Double(" + path.toString + ", " + value.toString + ")"
    case Event.Null(path) =>
      "Event.Null(" + path.toString + ")"
    case Event.End =>
      "Event.End"
  }
}

sealed trait ValueEvent[@specialized(scala.Long, scala.Boolean, scala.Double) T] extends Event {
  def path: Path[Any]
  def value: T
}

object Event {
  final case class Error(path: Path[Any], error: Predef.String)
    extends Event

  case object End extends Event

  final case class Null(path: Path[Any]) extends ValueEvent[scala.Null] {
    val value = null
  }

  final case class String(path: Path[Any], value: Predef.String) extends ValueEvent[Predef.String]
  final case class Long(path: Path[Any], value: scala.Long) extends ValueEvent[scala.Long]
  final case class Bool(path: Path[Any], value: scala.Boolean) extends ValueEvent[scala.Boolean]
  final case class Double(path: Path[Any], value: scala.Double) extends ValueEvent[scala.Double]
}


