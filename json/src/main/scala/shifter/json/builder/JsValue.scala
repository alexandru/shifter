package shifter.json.builder

import org.apache.commons.lang.StringEscapeUtils

sealed trait JsValue[@specialized(scala.Int, scala.Long, scala.Double, scala.Boolean) +T] {
  def value: T

  final lazy val compactPrint: String = export(false, 0)
  final lazy val prettyPrint: String = export(true, 0)

  private[builder] def export(isPretty: Boolean, level: Int): String
}

final case class JsInt(value: Int) extends JsValue[Int] {
  def export(isPretty: Boolean, level: Int) =
    value.toString
}

final case class JsLong(value: Long) extends JsValue[Long] {
  def export(isPretty: Boolean, level: Int) =
    value.toString
}

final case class JsDouble(value: Double) extends JsValue[Double] {
  def export(isPretty: Boolean, level: Int) =
    value.toString
}

final case class JsBool(value: Boolean) extends JsValue[Boolean] {
  def export(isPretty: Boolean, level: Int) =
    value.toString
}

final case class JsString(value: String) extends JsValue[String] {
  def export(isPretty: Boolean, level: Int) =
    "\"" + escapeJsonString(value) + "\""
}

case object JsNull extends JsValue[Nothing] {
  def value = throw new NoSuchElementException("JsNull")
  def export(isPretty: Boolean, level: Int) = "null"
}

final case class JsArray[T](value: Seq[JsValue[T]]) extends JsValue[Seq[JsValue[T]]] {
  def export(isPretty: Boolean, level: Int) = {
    val hasElements = !value.isEmpty

    val indentStr = if (isPretty) "    " * level else ""
    val builder =
      if (isPretty && hasElements)
        new StringBuilder("[\n")
      else
        new StringBuilder("[")

    val iterator = value.iterator

    while (iterator.hasNext) {
      val elem = iterator.next()

      if (isPretty && hasElements)
        builder.append(indentStr + "    ")

      builder.append(elem.export(isPretty, level + 1))

      if (iterator.hasNext)
        if (isPretty && hasElements)
          builder.append(",\n")
        else
          builder.append(',')
    }

    if (isPretty && hasElements)
      builder.append("\n" + indentStr + "]")
    else
      builder.append("]")

    builder.toString()
  }
}

final case class JsObj(value: Seq[(String, JsValue[_])]) extends JsValue[Seq[(String, JsValue[_])]] {
  def export(isPretty: Boolean, level: Int) = {
    val hasElements = !value.isEmpty

    val indentStr =
      if (isPretty)
        "    " * level
      else
        ""

    val builder =
      if (isPretty && hasElements)
        new StringBuilder("{\n")
      else
        new StringBuilder("{")

    val iterator = value.iterator

    while (iterator.hasNext) {
      val (key, value) = iterator.next()

      if (isPretty && hasElements)
        builder
          .append(indentStr)
          .append("    ")

      builder
        .append('"')
        .append(escapeJsonString(key))
        .append(if (isPretty) "\": " else "\":")
        .append(value.export(isPretty, level + 1))

      if (iterator.hasNext)
        if (isPretty && hasElements)
          builder.append(",\n")
        else
          builder.append(",")
    }

    if (isPretty && hasElements)
      builder.append("\n" + indentStr + "}")
    else
      builder.append("}")

    builder.toString()
  }
}
