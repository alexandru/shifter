package shifter

import java.net.URLDecoder._

/**
 * Started by: Alexandru Nedelcu
 * Copyright @2013 Epigrams, Inc.
 */
package object web {
  val IPFormat = "^(\\d{1,3}[.]\\d{1,3}[.]\\d{1,3}[.]\\d{1,3})$".r

  def urlDecode(data: String): Map[String, String] = {
    if (data != null && !data.isEmpty) {
      val parts = Seq(data.split("&"): _*)
      val values = parts.map(x => Seq(x.split("="): _*)).collect {
        case k :: v :: Nil => (decode(k, "UTF-8"), decode(v, "UTF-8"))
        case k :: Nil => (decode(k, "UTF-8"), "")
      }
      values.toMap
    }
    else
      Map.empty
  }

  def urlDecodeMulti(data: String): Map[String, Seq[String]] = {
    if (data != null && !data.isEmpty) {
      val parts = Seq(data.split("&"): _*)

      val keyVals = parts.map(x => Seq(x.split("="): _*)).collect {
        case k :: v :: Nil => (decode(k, "UTF-8"), decode(v, "UTF-8"))
        case k :: Nil => (decode(k, "UTF-8"), "")
      }

      keyVals.groupBy(_._1).map {
        case (k, list) => (k, list.map(_._2))
      }
    }
    else
      Map.empty
  }
}
