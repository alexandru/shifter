package shifter.web.api.mvc

import language.existentials
import java.util.regex.Pattern
import scala.util.matching.Regex
import scala.collection.mutable

/**
 * For pattern matching of paths using string interpolation.
 */
object PathMatcher {
  final class Path(parts: Seq[String]) {
    def unapplySeq (string: String) : Option[Seq[String]] =
      unapplyForPath(parts, string)
  }

  private[this] def unapplyForPath(parts: Seq[String], string: String) = {
    val partsArray = parts match {
      case obj: mutable.WrappedArray[_] =>
        obj.array.asInstanceOf[Array[String]]
      case _ =>
        parts.toArray
    }

    require(partsArray.length > 0)
    val first = partsArray(0)

    if (partsArray.length == 1) {
      if (first == string)
        SomeNil
      else if (
        first.length > 0 &&
          first.charAt(first.length - 1) == '/' &&
          string != null && string.length > 0 && string.charAt(string.length - 1) != '/' &&
          first.length == string.length + 1 &&
          first.startsWith(string))
        SomeNil
      else
        None
    }
    else if (partsArray.length > 1 && !string.startsWith(first))
      None

    else {
      val obj = cachedRegex(partsArray)
      val lastMatch = obj.lastMatch

      if (lastMatch != null && lastMatch._1 == first)
        Some(lastMatch._2)

      else {
        val result = obj.regex.unapplySeq(string)
        if (result.isDefined)
          obj.lastMatch = (string, result.get)
        result
      }
    }
  }

  private[this] def cachedRegex(parts: Array[String]): CacheValue = {
    val searchKey = CacheKey(parts)

    try
      regexCache(searchKey)

    catch {
      case _: NoSuchElementException =>
        synchronized {
          if (regexCache.contains(searchKey))
            cachedRegex(parts)

          else {
            val obj = CacheValue(buildRegex(parts))
            regexCache = regexCache.updated(searchKey, obj)
            obj
          }
        }
    }
  }

  private[this] def buildRegex(parts: Array[String]): Regex = {
    val regexBuilder = new StringBuilder("^")
    var rest: String = null

    require(parts.length > 1,
      s"shouldn't build a regex out of only ${parts.length} parts")
    require(ValidFirstPart.findFirstIn(parts.head).isDefined,
      s"invalid expression start (should not contain parens or curly braces)")

    val first = parts(0)

    var idx = 0
    while (idx < parts.length) {
      val thisPart = if (idx == 0 && first != null)
        first
      else
        parts(idx)

      if (rest != null) {
        regexBuilder.append(Pattern.quote(rest))
        rest = null
      }
      else
        regexBuilder.append(Pattern.quote(thisPart))

      if (idx + 1 < parts.length)
        parts(idx + 1) match {
          case part if part.isEmpty =>
            regexBuilder.append("([^/]+)")
          case PatternExtract(expr, path) =>
            if (expr != null)
              regexBuilder.append(s"($expr)")
            else
              regexBuilder.append("([^/]+)")
            rest = path
          case _ =>
            throw new IllegalArgumentException(s"invalid expression part: ${parts(idx+1)}")
        }

      idx += 1
    }

    if (parts.last.endsWith("/") && (parts.length > 1 || parts.last != "/"))
      regexBuilder.append("?")
    regexBuilder.append("$")

    regexBuilder.toString().trim.r
  }

  private[this] final class CacheKey(val array: Array[String]) {
    override lazy val hashCode: Int =
      array.toSeq.hashCode()

    override def equals(obj: Any): Boolean =
      if (obj == null)
        false
      else if (obj.isInstanceOf[CacheKey])
        java.util.Arrays.equals(
          array.asInstanceOf[Array[AnyRef]],
          obj.asInstanceOf[CacheKey].array.asInstanceOf[Array[AnyRef]]
        )
      else
        false

    override lazy val toString: String =
      s"CacheKey(${array.mkString(", ")})"
  }

  private[this] object CacheKey {
    def apply(array: Array[String]): CacheKey =
      new CacheKey(array)

    def apply(list: Seq[String]): CacheKey =
      list match {
        case obj: mutable.WrappedArray[_] =>
          new CacheKey(obj.array.asInstanceOf[Array[String]])
        case _ =>
          new CacheKey(list.toArray)
      }

    implicit object CacheKeyOrdering extends Ordering[CacheKey] {
      def compare(x: CacheKey, y: CacheKey): Int = {
        val arr1 = x.array
        val arr2 = y.array

        if (arr1.length < arr2.length)
          -1
        else if (arr1.length > arr2.length)
          1
        else {
          var finished = false
          var result = 0
          var idx = 0
          val length = arr1.length

          while (!finished && idx < length) {
            val cmp = cmpString.compare(arr1(idx), arr2(idx))

            if (cmp != 0) {
              result = cmp
              finished = true
            }
            else
              idx += 1
          }

          result
        }
      }
    }

    private[this] val cmpString = implicitly[Ordering[String]]
  }

  private[this] case class CacheValue(regex: Regex) {
    @volatile
    var lastMatch: (String, Seq[String]) = null
  }

  @volatile
  private[this] var regexCache = collection.immutable.SortedMap.empty[CacheKey, CacheValue]
  private[this] val SomeNil = Some(Nil)
  private[this] val PatternExtract = """^(?:\{([^(){}]+)\})?([^(){}]*)$""".r
  private[this] val ValidFirstPart = """^[^(){}]+$""".r
}

