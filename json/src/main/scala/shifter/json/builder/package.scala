package shifter.json

import org.apache.commons.lang.StringEscapeUtils

package object builder {
  trait NullableJsValue[T] {
    def of(v: Option[T]): JsValue[T]
  }

  implicit val NullableInt = new NullableJsValue[Int] {
    def of(v: Option[Int]) = v.map(JsInt.apply).getOrElse(JsNull)
  }

  implicit val NullableLong = new NullableJsValue[Long] {
    def of(v: Option[Long]) = v.map(JsLong.apply).getOrElse(JsNull)
  }

  implicit val NullableDouble = new NullableJsValue[Double] {
    def of(v: Option[Double]) = v.map(JsDouble.apply).getOrElse(JsNull)
  }

  implicit val NullableString = new NullableJsValue[String] {
    def of(v: Option[String]) = v.map(JsString.apply).getOrElse(JsNull)
  }

  implicit val NullableBool = new NullableJsValue[Boolean] {
    def of(v: Option[Boolean]) = v.map(JsBool.apply).getOrElse(JsNull)
  }

  def escapeJsonString(str: String) =
    StringEscapeUtils.escapeJavaScript(str).replace("\\'", "'")
}
