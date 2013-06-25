package shifter.json.builder

sealed trait JsValue[@specialized(scala.Int, scala.Long, scala.Double, scala.Boolean) +T] {
  def value: T

  final lazy val compactPrint: String = {
    val builder = new StringBuilder
    export(false, 0, builder)
    builder.toString()
  }

  final lazy val prettyPrint: String = {
    val builder = new StringBuilder
    export(true, 0, builder)
    builder.toString()
  }

  def export(isPretty: Boolean, level: Int, builder: StringBuilder)
}

final case class JsInt(value: Int) extends JsValue[Int] {
  def export(isPretty: Boolean, level: Int, builder: StringBuilder) {
    builder.append(value)
  }
}

final case class JsLong(value: Long) extends JsValue[Long] {
  def export(isPretty: Boolean, level: Int, builder: StringBuilder) {
    builder.append(value)
  }
}

final case class JsDouble(value: Double) extends JsValue[Double] {
  def export(isPretty: Boolean, level: Int, builder: StringBuilder) {
    builder.append(value)
  }
}

final case class JsBool(value: Boolean) extends JsValue[Boolean] {
  def export(isPretty: Boolean, level: Int, builder: StringBuilder) {
    builder.append(value)
  }
}

final case class JsString(value: String) extends JsValue[String] {
  def export(isPretty: Boolean, level: Int, builder: StringBuilder) {
    builder.append("\"").append(escapeJsonString(value)).append("\"")
  }
}

case object JsNull extends JsValue[Nothing] {
  def value = throw new NoSuchElementException("JsNull")
  def export(isPretty: Boolean, level: Int, builder: StringBuilder) {
    builder.append("null")
  }
}

final case class JsArray[T](value: Seq[JsValue[T]]) extends JsValue[Seq[JsValue[T]]] {
  def export(isPretty: Boolean, level: Int, builder: StringBuilder) {
    val hasElements = !value.isEmpty

    val indentStr = if (isPretty) "    " * level else ""
    if (isPretty && hasElements)
      builder.append("[\n")
    else
      builder.append("[")

    val iterator = value.iterator

    while (iterator.hasNext) {
      val elem = iterator.next()

      if (isPretty && hasElements)
        builder.append(indentStr + "    ")

      elem.export(isPretty, level + 1, builder)

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
  }
}

final case class JsObj(value: Seq[(String, JsValue[_])]) extends JsValue[Seq[(String, JsValue[_])]] {
  def export(isPretty: Boolean, level: Int, builder: StringBuilder) {
    val hasElements = !value.isEmpty

    val indentStr =
      if (isPretty)
        "    " * level
      else
        ""

    if (isPretty && hasElements)
      builder.append("{\n")
    else
      builder.append("{")

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

      value.export(isPretty, level + 1, builder)

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
  }
}
