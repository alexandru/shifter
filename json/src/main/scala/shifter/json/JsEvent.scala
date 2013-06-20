package shifter.json

import org.apache.commons.lang.StringEscapeUtils

sealed trait JsEvent {
  override lazy val toString = this match {
    case JsError(path, value) =>
      "JsError(" + path.toString + ", \"" + StringEscapeUtils.escapeJava(value) + "\")"
    case JsString(path, value) =>
      "JsString(" + path.toString + ", \"" + StringEscapeUtils.escapeJava(value) + "\")"
    case JsLong(path, value) =>
      "JsLong(" + path.toString + ", " + value.toString + ")"
    case JsBoolean(path, value) =>
      "JsBoolean(" + path.toString + ", " + value.toString + ")"
    case JsDouble(path, value) =>
      "JsDouble(" + path.toString + ", " + value.toString + ")"
    case JsEnd =>
      "JsEnd"
  }
}

final case class JsError(path: Path[Any], error: String)
  extends JsEvent

case object JsEnd extends JsEvent

sealed trait JsValue[@specialized(scala.Long, scala.Boolean, scala.Double) T] extends JsEvent {
  def path: Path[Any]
  def value: T
}

case class JsString(path: Path[Any], value: String) extends JsValue[String]
case class JsLong(path: Path[Any], value: Long) extends JsValue[Long]
case class JsBoolean(path: Path[Any], value: Boolean) extends JsValue[Boolean]
case class JsDouble(path: Path[Any], value: Double) extends JsValue[Double]



