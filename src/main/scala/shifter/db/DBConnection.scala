package shifter.db

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import shifter.reflection._


class DBConnectionException(msg: String) extends SQLException(msg)

class DBConnection(val underlying: Connection) {
  def withinTransaction[A](f: => A): A = {
    try {
      underlying.setAutoCommit(false)
      val ret = f
      underlying.commit()
      ret
    }
    catch {
      case ex: Throwable =>
	underlying.rollback()
	throw ex
    }
    finally {
      underlying.setAutoCommit(true)
    }
  }

  def adapter = {
    val url = underlying.getMetaData.getURL
    val dbIdentifier = DBConnection.identifierOf(url)
    DBConnection.adapterForUrl(url).getOrElse {      
      throw new DBConnectionException("Cannot find adapter for DB type '" + dbIdentifier + "'")
    }
  }

  def listTables: Seq[String] =
    adapter.listTables(underlying)

  def fetchOne(sql: String, args: String*): Option[String] = 
    using(underlying.prepareStatement(sql)) {
      stm =>
	(0 until args.length).foreach {
	  idx =>
	    stm.setString(idx+1, args(idx))
	}
	using (stm.executeQuery) {
	  rs => if (rs.next) Some(rs.getString(1)) else None
	}
    }

  def changeSchema(sql: String, silentOnErrors: Boolean = false) = 
    try {
      using (underlying.createStatement)(_.execute(sql))
    } catch {
      case ex: Exception =>
	if (!silentOnErrors)
	  throw ex
    }
}

object DBConnection {
  @volatile
  private[this] var registeredPackages = Set.empty[String]
  @volatile
  private[this] var registeredAdapters = Map.empty[String, DBAdapter]

  private[this] val JDBCUrl = """^jdbc:(\w+):.*$""".r

  def registerPackage(name: String) {
    this.synchronized {
      registeredPackages += name
    }
  }

  def adapterFor(dbIdentifier: String): Option[DBAdapter] = 
    if (registeredAdapters.contains(dbIdentifier))
      Some(registeredAdapters(dbIdentifier))
    else
      this.synchronized {
	if (registeredAdapters.contains(dbIdentifier))
	  adapterFor(dbIdentifier)
	else {
	  val classes = findSubTypes[DBAdapter](registeredPackages + "shifter.db").filterNot(_.isInterface)

	  classes.foldLeft(Option.empty[DBAdapter]) { 
	    (acc, cls) =>
	      if (acc.isEmpty)
		toInstance(cls) match {
		  case Some(inst) if inst.dbIdentifier == dbIdentifier =>
		    registeredAdapters += (dbIdentifier -> inst)
		    Some(inst)
		  case _ => None
		}
	      else acc
	  }
	}
      }

  def identifierOf(url: String) = 
    url match {
      case JDBCUrl(dbIdentifier) => dbIdentifier
      case _ => throw new IllegalArgumentException("Invalid URL syntax for in '" + url + "'")
    }

  def adapterForUrl(url: String): Option[DBAdapter] = 
    adapterFor(identifierOf(url))

  def apply(url: String, user: String, password: String): Connection = 
    adapterForUrl(url) match {
      case Some(inst) => inst.initConnection(url, user, password)
      case None =>
	  throw new DBConnectionException("Cannot find adapter for DB type '" + identifierOf(url) + "'")
    }
}
