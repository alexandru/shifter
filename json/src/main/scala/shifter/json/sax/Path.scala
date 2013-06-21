package shifter.json.sax

import scala.annotation.tailrec
import org.apache.commons.lang.StringEscapeUtils

sealed trait Path[+T]  {
  def \[U >: T](part: U): \[U] = shifter.json.sax.\(this, part)

  def pop: Path[T] = this match {
    case Root => throw new NoSuchElementException("Root.pop")
    case rest \ last => rest
  }

  def pop2: (T, Path[T]) = this match {
    case Root => throw new NoSuchElementException("Root.pop")
    case rest \ last => (last, rest)
  }

  override lazy val toString = {
    @tailrec
    def loop(path: Path[T], acc: String): String = path match {
      case rest \ Index(last) =>
        if (!acc.isEmpty)
          loop(rest, last.toString + " \\ " + acc)
        else
          loop(rest, last.toString)
      case rest \ last =>
        if (!acc.isEmpty)
          loop(rest, "\"" + StringEscapeUtils.escapeJava(last.toString) + "\" \\ " + acc)
        else
          loop(rest, "\"" + StringEscapeUtils.escapeJava(last.toString) + "\"")
      case Root =>
        if (acc.isEmpty)
          "Root"
        else
          "Root \\ " + acc
    }

    loop(this, "")
  }

  def toList: List[T] = {
    @tailrec
    def loop(path: Path[T], acc: List[T]): List[T] = path match {
      case rest \ last =>
        loop(rest, last :: acc)
      case Root =>
        acc
    }

    loop(this, List.empty)
  }

  def toStream: Stream[T] = {
    def loop(path: Path[T]): Stream[T] = path match {
      case rest \ last =>
        last #:: loop(rest)
      case Root =>
        Stream.empty[T]
    }

    loop(this)
  }
}

case object Root extends Path[Nothing]
case class \[+T](path: Path[T], part: T) extends Path[T]

object Path {
  def empty[T]: Path[T] = Root.asInstanceOf[Path[T]]

  def apply[T](parts: T*): Path[T] =
    parts.foldLeft(Path.empty[T])((acc, e) => acc \ e)
}

object Index {
  def unapply(x: Any): Option[Int] =
    x match {
      case x: Int => Some(x)
      case _ => None
    }
}