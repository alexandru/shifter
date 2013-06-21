package shifter.json.sax

import org.apache.commons.lang.StringEscapeUtils

sealed trait JsonEvent {
  override lazy val toString = this match {
    case JsonError(path, value) =>
      "JsonError(" + path.toString + ", \"" + StringEscapeUtils.escapeJava(value) + "\")"
    case JsStringEvent(path, value) =>
      "JsStringEvent(" + path.toString + ", \"" + StringEscapeUtils.escapeJava(value) + "\")"
    case JsLongEvent(path, value) =>
      "JsLongEvent(" + path.toString + ", " + value.toString + ")"
    case JsBooleanEvent(path, value) =>
      "JsBooleanEvent(" + path.toString + ", " + value.toString + ")"
    case JsDoubleEvent(path, value) =>
      "JsDoubleEvent(" + path.toString + ", " + value.toString + ")"
    case JsonEnd =>
      "JsonEnd"
  }
}

final case class JsonError(path: Path[Any], error: String)
  extends JsonEvent

case object JsonEnd extends JsonEvent

sealed trait JsValueEvent[@specialized(scala.Long, scala.Boolean, scala.Double) T] extends JsonEvent {
  def path: Path[Any]
  def value: T
}

case class JsStringEvent(path: Path[Any], value: String) extends JsValueEvent[String]
case class JsLongEvent(path: Path[Any], value: Long) extends JsValueEvent[Long]
case class JsBooleanEvent(path: Path[Any], value: Boolean) extends JsValueEvent[Boolean]
case class JsDoubleEvent(path: Path[Any], value: Double) extends JsValueEvent[Double]



