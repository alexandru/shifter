package shifter.json.builder

import scala.collection.mutable

object Json {
  def obj(elems: (String, JsValue[Any])*): JsObj =
    JsObj(elems.toIndexedSeq)
  def obj[T](elems: (String, T)*)(implicit ev: NullableJsValue[T]) =
    JsObj(elems.toIndexedSeq.map { case (k,v) => (k, ev.of(Option(v))) })

  def arr[T](elems: JsValue[T]*): JsArray[T] = JsArray(elems.toIndexedSeq)
  def arr[T](elems: T*)(implicit ev: NullableJsValue[T]): JsArray[T] =
    JsArray(elems.toIndexedSeq.map(x => ev.of(Option(x))))

  val none = JsNull

  def of[T](o: Option[T])(implicit ev: NullableJsValue[T]): JsValue[T] = ev.of(o)
  def of[T](o: T)(implicit ev: NullableJsValue[T]): JsValue[T] = ev.of(Option(o))

  def int(i: Int): JsInt = JsInt(i)
  def int(i: Option[Int])(implicit ev: NullableJsValue[Int]): JsValue[Int] = ev.of(i)

  def long(i: Long): JsLong = JsLong(i)
  def long(i: Option[Long])(implicit ev: NullableJsValue[Long]): JsValue[Long] = ev.of(i)

  def bool(i: Boolean): JsBool = JsBool(i)
  def bool(i: Option[Boolean])(implicit ev: NullableJsValue[Boolean]): JsValue[Boolean] = ev.of(i)

  def str(i: String): JsString = JsString(i)
  def str(i: Option[String])(implicit ev: NullableJsValue[String]): JsValue[String] = ev.of(i)

  def double(i: Double): JsDouble = JsDouble(i)
  def double(i: Option[Double])(implicit ev: NullableJsValue[Double]): JsValue[Double] = ev.of(i)
}
