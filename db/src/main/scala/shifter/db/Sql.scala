package shifter.db

import java.util.Date
import java.util.Calendar
import java.io.InputStream
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import collection.breakOut
import reflect.Manifest
import shifter.reflection.castTo


class SqlException(msg: String) extends RuntimeException(msg)


class Row(val names: Vector[String], val values: Vector[Any]) {
  lazy val toMap =
    names.zip(values).toMap

  private[this] lazy val namesSet =
    names.toSet

  def apply[T : Manifest](key: String): T = {
    val keys = Seq(key, key.toLowerCase, key.toUpperCase)
    val searchKey = keys.find(namesSet.contains(_))
    require(!searchKey.isEmpty, "Non-existent key '"+ key +"' in row")
    castTo[T](toMap(searchKey.get)) match {
      case Some(value) => value
      case None =>
	throw new IllegalArgumentException("Cannot find element for key '"+key+"' of type "+manifest[T].toString)
    }
  }

  def get[T : Manifest](key: String): Option[T] = {
    val keys = Seq(key, key.toLowerCase, key.toUpperCase)
    keys.find(namesSet.contains(_)) match {
      case Some(key) => 
	val value = toMap(key)
	castTo[T](value)
      case None =>
	None
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

sealed class Sql(conn: Connection, query: String) {  

  protected def args: Seq[Any] = Seq.empty[Any]

  protected def preparedQuery: String = query

  def update(): Int = {
    val stm = conn.prepareStatement(preparedQuery)
    try {
      args.foldLeft(0) { (idx, obj) =>
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
      args.foldLeft(0) { (idx, obj) =>
	setStatementValue(stm, idx, obj)
	idx + 1
      }
      stm.execute      
    } 
    finally {
      stm.close()
    }
  }  

  def select: Iterator[Row] = 
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
	      case value : java.sql.Timestamp =>
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

      private[this] val stm = 
	conn.prepareStatement(preparedQuery)

      private[this] val lazyResult = {
	try {
	  args.foldLeft(0) { 
	    (idx, obj) =>
	      setStatementValue(stm, idx, obj)
	    idx + 1
	  }      

	  val rs = stm.executeQuery
	  val meta = rs.getMetaData
	  val colsCount = meta.getColumnCount
	  val names = (1 to colsCount).map(meta.getColumnName(_))(breakOut) : Vector[String]
	  (names, colsCount, rs)	  
	}
	catch {
	  case ex: Throwable =>
	    if (!stm.isClosed) stm.close()
	    throw ex
	}
      }
    }

  def withArgs(args: Any*) = 
    Sql(conn, query, args)

  def withNamedArgs(args: (String, Any)*) =
    Sql(conn, query, args.toMap)

  def on(head: (String, Any), tail: (String, Any)*): Sql =
    Sql(conn, query, (head +: tail).toMap)

  private[this] def setStatementValue(stm: PreparedStatement, idx: Int, obj: Any) {
    obj match {
      case value : Int =>
	stm.setInt(idx+1, value)
      case value : Double =>
	stm.setDouble(idx+1, value)
      case value : Float =>
	stm.setFloat(idx+1, value)
      case value : Calendar =>
	stm.setString(idx+1, value)
      case value : Date =>
	stm.setString(idx+1, value)
      case value : InputStream =>
	stm.setBlob(idx+1, value)
      case value : Array[Byte] =>
	stm.setBytes(idx+1, value)
      case value : Boolean =>
	stm.setBoolean(idx+1, value)
      case value : java.net.URL =>
	stm.setURL(idx+1, value)
      case value : String =>
	stm.setString(idx+1, value)
      case x =>
	throw new IllegalArgumentException(if (x == null) "null" else x.toString)
    }
  }

  implicit private[this] def `Calendar -> String`(cal: Calendar): String = 
    dateParser.format(cal.getTime)

  implicit private[this] def `Date -> String`(date: Date): String = 
    dateParser.format(date)

  private[this] val dateParser = 
    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
}

object Sql {
  def apply(conn: Connection, query: String, args: Seq[Any]): Sql =
    new SqlWithIndexedArgs(conn, query, args)    

  def apply(conn: Connection, query: String, args: Map[String, Any]): Sql =
    new SqlWithNamedArgs(conn, query, args)    

  private class SqlWithIndexedArgs(conn: Connection, query: String, iArgs: Seq[Any]) extends Sql(conn, query) {
    override protected val args = iArgs

    override def withArgs(args: Any*) = 
      Sql(conn, query, iArgs ++ args)

    override def on(head: (String, Any), tail: (String, Any)*): Sql = 
      throw new SqlException("SQL template with indexed arguments cannot be converted to one with named arguments")
  }

  private class SqlWithNamedArgs(conn: Connection, query: String, nArgs: Map[String, Any], parsed: (String, Vector[String])) extends Sql(conn, query) {
    def this(conn: Connection, query: String, nArgs: Map[String, Any]) =
      this(conn, query, nArgs, parseQuery(query))

    override protected lazy val preparedQuery =
      parsed._1

    override lazy val args = 
      parsed._2.map(x => nArgs.get(x) match {
	case Some(x) => x
	case None => throw new SqlException("Argument for named param "+ x +" not given")
      })

    override def withArgs(args: Any*) = 
      throw new SqlException("SQL template with named arguments cannot be converted to one with indexed arguments")

    override def withNamedArgs(args: (String, Any)*) =
      if (args.length == 0)
	this
      else
	new SqlWithNamedArgs(conn, query, nArgs ++ args.toMap, parsed)

    override def on(head: (String, Any), tail: (String, Any)*): Sql = {
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
      else if (!inParam && ch == ':') {
	inParam = true
	currentElem = ""
	cIdx = idx - 1
      }
      else if (inParam && paramChars.contains(ch)) {
	currentElem += ch
      }
      else if (inParam && ch == ':')
	throw new SqlException("Invalid syntax for named parameter "+ currentElem +" at char "+ (idx-1))
      else if (inParam) {
	discovered = discovered :+ (currentElem -> cIdx)
	inParam = false
	currentElem = ""
	cIdx = -1
      }
      else
	Unit      
    }

    if (!currentElem.isEmpty && cIdx > -1)
      discovered = discovered :+ (currentElem -> cIdx)
    
    val newQuery = discovered.reverse.foldLeft(query) {
      (query, elem) =>
	val (name, idx) = elem
      query.substring(0, idx) + "?" + query.substring(idx + 1 + name.length)
    }

    (newQuery, discovered.map(_._1))
  }
}

object SQL {
  def apply(query: String)(implicit db: Connection): Sql = 
    new Sql(db, query)
}
