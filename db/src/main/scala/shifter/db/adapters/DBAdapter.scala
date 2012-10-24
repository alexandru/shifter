package shifter.db.adapters

import shifter.lang.memoize
import shifter.reflection._
import shifter.lang.backports._
import java.sql.Connection
import java.sql.DriverManager


class DBAdapter(val dbIdentifier: String) {
  def initConnection(url: String, user: String, password: String): Connection = 
    DriverManager.getConnection(url, user, password)

  def listTables(conn: Connection): Seq[String] = {
    val rs = conn.getMetaData.getTables(null, null, "%", null)
    var tables = Seq.empty[String]    
    while (rs.next) 
      tables = tables :+ rs.getString(3) 
    tables
  }
}


object DBAdapter {
  def registerPackage(name: String) {
    this.synchronized {
      registeredPackages += name
    }
  }

  def allAdapterClasses = 
    memoize("shifter.db.DBAdapter.allAdapterClasses", registeredPackages.size) {
      findSubTypes[DBAdapter](registeredPackages + "shifter.db.adapters").filterNot(_.isInterface)
    }
  
  def adapterFor(dbIdentifier: String): DBAdapter = 
    memoize("shifter.db.DBAdapter.adapterFor", dbIdentifier) {
      val classes = allAdapterClasses
      val adapter = classes.foldLeft(Option.empty[DBAdapter]) { 
	(acc, cls) =>
	  if (acc.isEmpty)
	    toInstance(cls) match {
	      case Some(inst) if inst.dbIdentifier == dbIdentifier =>
		Some(inst)
	      case _ => None
	    }
	  else acc
      }

      adapter match {
	case Some(inst) => inst
	case None => new DBAdapter(dbIdentifier)
      }
    }

  def identifierOf(url: String) = 
    url match {
      case JDBCUrl(dbIdentifier) => dbIdentifier
      case _ => throw new IllegalArgumentException("Invalid URL syntax for in '" + url + "'")
    }

  def adapterForUrl(url: String): DBAdapter = 
    adapterFor(identifierOf(url))

  def adapterForConnection(conn: Connection): DBAdapter = 
    adapterForUrl(conn.getMetaData.getURL)

  def apply(url: String, user: String, password: String): Connection = 
    adapterForUrl(url).initConnection(url, user, password)

  @volatile
  private[this] var registeredPackages = Set.empty[String]
  private[this] val JDBCUrl = """^jdbc:(\w+):.*$""".r
}
