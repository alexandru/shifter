package shifter

import shifter.lang._
import java.io.{File, FileInputStream}
import java.util.zip.ZipInputStream
import collection.JavaConverters._
import annotation.tailrec

package object reflection {

  def allTypesIn(packages: Set[String]): Set[Class[_]] = {
    val classLoader = Thread.currentThread().getContextClassLoader()
    assert(classLoader != null, "Problem loading ClassLoader")

    val paths = packages.map(_.replace('.', '/'))
    val resources = paths.flatMap(p => classLoader.getResources(p).asScala.toSeq).toList.map(_.toExternalForm)
    val classFiles: Set[String] = resources.flatMap(r => listClasses(r, packages)).toSet
    classFiles.flatMap(p => toClass(p))    
  }

  def findSubTypes[T : Manifest](packages: Set[String]): Set[Class[T]] = {
    val m = manifest[T].erasure
    val classes = allTypesIn(packages)
    
    val found = classes.filter(c => m != c && m.isAssignableFrom(c))
    found.asInstanceOf[Set[Class[T]]]
  }    

  def findSubTypes[T : Manifest](packageName: String): Set[Class[T]] = 
    findSubTypes[T](Set(packageName))

  def listClasses(directory: String, packages: Set[String]): List[String] = {
    def extractor(pkgName: String) = 
      ("^.*\\W(" + pkgName.replace(".", "\\W") + """\W.*)\.class$""").r

    val sortedPackages = packages.toList.sortWith((a,b) => a.length > b.length || a <= b)

    listFiles(directory).flatMap {
      path =>
	val possibilities = sortedPackages.flatMap {
	  packageName =>
	    val ClassName = extractor(packageName)
	    path match {
	      case ClassName(name) => Some(name.replace('/', '.'))
	      case _ => None
	    }
	}
	
	possibilities.headOption
    }
  }

  def listFiles(directory: String): List[String] = {
    val JarFile = """^jar:file:(/[^!]+\.jar)!/(.*)$""".r
    val DiskFile = """^(?:file:)?(/.*)$""".r

    directory match {
      case JarFile(jarPath, dirPath) => 
	using (new ZipInputStream(new FileInputStream(jarPath))) { zip =>
          var hasEntries = true
	  var entries = List.empty[String]
          while (hasEntries) {
	    val entry = zip.getNextEntry
            if (entry != null) {
	      if (entry.getName.startsWith(dirPath))
		entries = entries :+ ("jar:file:" + jarPath + "!/" + entry.getName)
	    }
	    else
	      hasEntries = false
	  }
          entries
	}
      case DiskFile(path) => 
	val file = new File(path)
	if (file.isDirectory)
	  new File(path).listFiles.toList.map(_.getAbsolutePath).flatMap {
	    p => listFiles(p)
	  }
	else
	  List(path)
      case _ =>
	throw new IllegalArgumentException("Don't know how to open '" + directory + "'")
    }
  }

  def allSuperTypes(cls: Class[_]): Set[Class[_]] = {
    val superTypes = cls.getInterfaces.toSet ++ Option(cls.getSuperclass)
    Set(cls) ++ superTypes.flatMap(c => allSuperTypes(c))
  }

  def toClass(name: String): Option[Class[_]] =
    try {
      Some(Class.forName(name))
    } catch {
      case _: ClassNotFoundException => None
      case _: NoClassDefFoundError => None
    }

  def toInstance[T](cls: Class[T], anyArgs: Any*): Option[T] = {
    @tailrec
    def checkEquiv(required: Seq[Class[_]], given: Seq[Class[_]]): Boolean =
      if (required.size != given.size) false
      else
	required match {
	  case rhead :: rtail =>
	    if (!rhead.isAssignableFrom(given.head))
	      false
	    else
	      checkEquiv(rtail, given.tail)
	  case _ =>
	    true
	}

    try {
      val args = anyArgs.map(_.asInstanceOf[Object])
      val given = args.map(_.getClass)

      val constructor = cls.getConstructors().find(c => {
	val required = c.getParameterTypes()
	checkEquiv(required, given)
      })

      constructor match {
	case Some(cons) => 
	  if (cons.getParameterTypes().length == 0)
	    Some(cons.newInstance().asInstanceOf[T])
	  else
	    Some(cons.newInstance(args :_*).asInstanceOf[T])
	case _ => None
      }
    } 
    catch {
      case _ => None
    }
  }
}
