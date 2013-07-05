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

final case class JsArray[T](value: IndexedSeq[JsValue[T]]) extends JsValue[IndexedSeq[JsValue[T]]] {
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

final case class JsObj(value: IndexedSeq[(String, JsValue[_])]) extends JsValue[IndexedSeq[(String, JsValue[_])]] {
  def +[T](elem: (String, T))(implicit ev: NullableJsValue[T]) =
    JsObj(value :+ (elem._1, ev.of(Option(elem._2))))

  def +(elem: (String, JsValue[_])) =
    JsObj(value :+ elem)

  def ++(other: JsObj) =
    JsObj(value ++ other.value)

  def updated(k: String, v: JsValue[_]) =
    JsObj(value :+ (k, v))

  def updated[T](k: String, v: T)(implicit ev: NullableJsValue[T]) =
    JsObj(value :+ (k, ev.of(Option(v))))

  def export(isPretty: Boolean, level: Int, builder: StringBuilder) {
    val hasElements = !value.isEmpty
    val length = value.length

    val skipKeys = {
      val arr = new Array[Boolean](length)
      val alreadyPicked = collection.mutable.Set.empty[String]

      var idx = 0
      while (idx < length) {
        val arrIdx = length - idx - 1
        val key = value(arrIdx)._1
        if (alreadyPicked(key))
          arr(arrIdx) = true
        else {
          arr(arrIdx) = false
          alreadyPicked += key
        }
        idx += 1
      }

      arr
    }

    val indentStr =
      if (isPretty)
        "    " * level
      else
        ""

    if (isPretty && hasElements)
      builder.append("{\n")
    else
      builder.append("{")

    var idx = 0
    while (idx < length) {
      if (!skipKeys(idx)) {
        val (key, value) = this.value(idx)
        if (isPretty && hasElements)
          builder
            .append(indentStr)
            .append("    ")

        builder
          .append('"')
          .append(escapeJsonString(key))
          .append(if (isPretty) "\": " else "\":")

        value.export(isPretty, level + 1, builder)

        if (idx < length - 1)
          if (isPretty && hasElements)
            builder.append(",\n")
          else
            builder.append(",")
      }

      idx += 1
    }

    if (isPretty && hasElements)
      builder.append("\n" + indentStr + "}")
    else
      builder.append("}")
  }
}
