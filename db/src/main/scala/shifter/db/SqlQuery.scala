package shifter.db

import language.implicitConversions
import java.util.Date
import java.util.Calendar
import java.io.InputStream
import java.sql.Connection
import java.sql.PreparedStatement
import collection.breakOut
import scala.util.control.NonFatal
import scala.reflect.runtime.universe._
import scala.util.{Success, Failure, Try}


class SqlException(msg: String) extends RuntimeException(msg)

class Row(val names: Vector[String], val values: Vector[Any]) {
  lazy val toMap =
    names.zip(values).toMap

  private[this] lazy val namesSet =
    names.toSet

  def apply[T : DBCodec : TypeTag](key: String): T =
    get[T](key) match {
      case Success(x) => x
      case Failure(ex) => throw ex
    }

  def get[T : DBCodec : TypeTag](key: String): Try[T] = {
    val keys = Seq(key, key.toLowerCase, key.toUpperCase)
    val ev = implicitly[DBCodec[T]]

    keys.find(namesSet) match {
      case Some(k) =>
        val value = toMap.getOrElse(k, null)
        ev.fromDB(value)
      case None =>
        Failure(new NoSuchElementException(s"Key `$key` is not part of the resultset"))
    }
  }
}

object Row {
  def unapplySeq(row: Row): Option[Seq[Any]] =
    if (row.values.size > 0)
      Some(row.values)
    else
      None
}

sealed class SqlQuery(conn: Connection, query: String) {

  protected def args: Seq[Any] = Seq.empty[Any]

  protected def preparedQuery: String = query

  def update(): Int = {
    val stm = conn.prepareStatement(preparedQuery)
    try {
      args.foldLeft(0) {
        (idx, obj) =>
          setStatementValue(stm, idx, obj)
          idx + 1
      }
      stm.executeUpdate
    }
    finally {
      stm.close()
    }
  }

  def execute() {
    val stm = conn.prepareStatement(preparedQuery)
    try {
      args.foldLeft(0) {
        (idx, obj) =>
          setStatementValue(stm, idx, obj)
          idx + 1
      }
      stm.execute
    }
    finally {
      stm.close()
    }
  }

  def select[A](f: Iterator[Row] => A) = {
    val stm =
      conn.prepareStatement(preparedQuery)

    val result = try {
      args.foldLeft(0) {
        (idx, obj) =>
          setStatementValue(stm, idx, obj)
          idx + 1
      }

      val rs = stm.executeQuery
      val meta = rs.getMetaData
      val colsCount = meta.getColumnCount
      val names = (1 to colsCount).map(meta.getColumnName)(breakOut): Vector[String]
      (names, colsCount, rs)
    }
    catch {
      case ex: Throwable =>
        if (!stm.isClosed) stm.close()
        throw ex
    }

    try {
      f(iter(stm, result))
    }
    finally {
      try {
        result._3.close()
      } catch {
        case NonFatal(_) =>
      }
      if (!stm.isClosed) stm.close()
    }
  }

  def first[A](f: Row => A): Option[A] =
    select(i => if (i.hasNext) Some(f(i.next())) else None)

  def map[A](f: Row => A): Seq[A] =
    select(i => i.map(f).toList)

  def foldLeft[A](initialValue: A)(f: (A, Row) => A): A =
    select(i => i.foldLeft(initialValue)(f))

  private[this] def iter(stm: PreparedStatement, lazyResult: (Vector[String], Int, java.sql.ResultSet)): Iterator[Row] =
    new Iterator[Row] {
      def hasNext =
        if (doesHaveNext)
          true
        else if (noMore)
          false
        else if (result.next) {
          doesHaveNext = true
          true
        }
        else {
          noMore = true
          if (!stm.isClosed)
            stm.close()
          false
        }

      def next(): Row = {
        assert(!noMore, "Iterator.empty")
        if (!doesHaveNext)
          doesHaveNext = result.next
        assert(doesHaveNext, "Iterator.empty")

        val cols = (1 to colsCount).foldLeft(Vector.empty[Any]) {
          (acc, idx) =>
            val obj = result.getObject(idx) match {
              case value: java.sql.Timestamp =>
                new Date(value.getTime)
              case value => value
            }
            acc :+ obj
        }

        doesHaveNext = false
        new Row(names, cols)
      }

      private[this] var doesHaveNext = false
      private[this] var noMore = false

      private[this] def names = lazyResult._1

      private[this] def colsCount = lazyResult._2

      private[this] def result = lazyResult._3
    }

  def withArgs(args: Any*) =
    SqlQuery(conn, query, args)

  def withNamedArgs(args: (String, Any)*) =
    SqlQuery(conn, query, args.toMap)

  def on(head: (String, Any), tail: (String, Any)*): SqlQuery =
    SqlQuery(conn, query, (head +: tail).toMap)

  private[this] def setStatementValue(stm: PreparedStatement, idx: Int, obj: Any) {
    obj match {
      case value: Int =>
        stm.setInt(idx + 1, value)
      case value: Double =>
        stm.setDouble(idx + 1, value)
      case value: Float =>
        stm.setFloat(idx + 1, value)
      case value: Calendar =>
        stm.setString(idx + 1, value)
      case value: Date =>
        stm.setString(idx + 1, value)
      case value: InputStream =>
        stm.setBlob(idx + 1, value)
      case value: Array[Byte] =>
        stm.setBytes(idx + 1, value)
      case value: Boolean =>
        stm.setBoolean(idx + 1, value)
      case value: java.net.URL =>
        stm.setURL(idx + 1, value)
      case value: String =>
        stm.setString(idx + 1, value)
      case null =>
        stm.setNull(idx + 1, java.sql.Types.VARCHAR)
      case x =>
        throw new IllegalArgumentException(x.toString)
    }
  }

  implicit private[this] def `Calendar -> String`(cal: Calendar): String =
    dateParser.format(cal.getTime)

  implicit private[this] def `Date -> String`(date: Date): String =
    dateParser.format(date)

  private[this] val dateParser =
    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
}

object SqlQuery {
  def apply(conn: Connection, query: String, args: Seq[Any]): SqlQuery =
    new SqlWithIndexedArgs(conn, query, args)

  def apply(conn: Connection, query: String, args: Map[String, Any]): SqlQuery =
    new SqlWithNamedArgs(conn, query, args)

  private class SqlWithIndexedArgs(conn: Connection, query: String, iArgs: Seq[Any]) extends SqlQuery(conn, query) {
    override protected val args = iArgs

    override def withArgs(args: Any*) =
      SqlQuery(conn, query, iArgs ++ args)

    override def on(head: (String, Any), tail: (String, Any)*): SqlQuery =
      throw new SqlException("SQL template with indexed arguments cannot be converted to one with named arguments")
  }

  private class SqlWithNamedArgs(conn: Connection, query: String, nArgs: Map[String, Any], parsed: (String, Vector[String])) extends SqlQuery(conn, query) {
    def this(conn: Connection, query: String, nArgs: Map[String, Any]) =
      this(conn, query, nArgs, parseQuery(query))

    override protected lazy val preparedQuery =
      parsed._1

    override lazy val args =
      parsed._2.map(x => nArgs.get(x) match {
        case Some(value) => value
        case None => throw new SqlException("Argument for named param " + x + " not given")
      })

    override def withArgs(args: Any*) =
      throw new SqlException("SQL template with named arguments cannot be converted to one with indexed arguments")

    override def withNamedArgs(args: (String, Any)*) =
      if (args.length == 0)
        this
      else
        new SqlWithNamedArgs(conn, query, nArgs ++ args.toMap, parsed)

    override def on(head: (String, Any), tail: (String, Any)*): SqlQuery = {
      val addArgs = tail.toMap + head
      new SqlWithNamedArgs(conn, query, nArgs ++ addArgs, parsed)
    }
  }

  private[this] def parseQuery(query: String): (String, Vector[String]) = {
    val paramChars = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ List('_', '-')).toSet

    val length = query.length
    var idx = 0
    var quoteCh: Char = 0
    var inEscape = false

    var ch: Char = 0
    var inParam = false
    var currentElem = ""
    var cIdx = -1

    var discovered = Vector.empty[(String, Int)]

    while (idx < length) {
      ch = query(idx)
      idx = idx + 1

      if (inEscape)
        inEscape = false
      else if (ch == '\\')
        inEscape = true
      else if (ch == quoteCh)
        quoteCh = 0
      else if (ch == '"' || ch == '\'' || ch == '`')
        quoteCh = ch
      else if (quoteCh != 0)
        Unit
      else if (ch == '?')
        throw new SqlException("SQL syntax with ? placeholders is not supported in combination with named arguments")
      else if (!inParam && ch == '{') {
        inParam = true
        currentElem = ""
        cIdx = idx - 1
      }
      else if (inParam)
        if (ch == '}')
          if (currentElem.length > 0) {
            discovered = discovered :+ (currentElem -> cIdx)
            inParam = false
            currentElem = ""
            cIdx = -1
          }
          else
            throw new SqlException("Invalid syntax at char " + (idx - 1))
        else if (!paramChars.contains(ch))
          throw new SqlException("Invalid syntax for named parameter " + currentElem + " at char " + (idx - 1))
        else
          currentElem += ch
      else
        Unit
    }

    if (!currentElem.isEmpty && cIdx > -1)
      discovered = discovered :+ (currentElem -> cIdx)

    val newQuery = discovered.reverse.foldLeft(query) {
      (query, elem) =>
        val (name, idx) = elem
        query.substring(0, idx) + "?" + query.substring(idx + 2 + name.length)
    }

    (newQuery, discovered.map(_._1))
  }
}

object SQL {
  def apply(query: String)(implicit db: Connection): SqlQuery =
    new SqlQuery(db, query)
}
