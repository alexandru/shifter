package com.bionicspirit

import java.io.{File, FileInputStream}
import java.util.zip.ZipInputStream
import collection.JavaConversions.enumerationAsScalaIterator

package object utils {
  type Closeable = { def close():Unit }

  def using[A, B <: Closeable](closable: B)(f: B => A): A = 
    try {
      f(closable)
    }
    finally {
      closable.close()
    }

  def findTypes[T : Manifest](packageName: String): Set[Class[T]] = {
    val m = manifest[T].erasure

    val classLoader = Thread.currentThread().getContextClassLoader()
    assert(classLoader != null, "Problem loading ClassLoader")

    val path = packageName.replace('.', '/')
    val resources = classLoader.getResources(path).toList.map(_.toExternalForm)
    val classFiles: Set[String] = resources.flatMap(r => listClasses(r, packageName)).toSet
    val classes = classFiles.flatMap(p => toClass(p))

    val found = classes.filter(c => m != c && allSuperTypes(c).contains(m))
    found.asInstanceOf[Set[Class[T]]]
  }

  def listClasses(directory: String, packageName: String): List[String] = {
    val ClassName = ("^.*\\W(" + packageName.replace(".", "\\W") + """\W.*)\.class$""").r
    listFiles(directory).collect {
      case ClassName(name) => name.replace('/', '.')
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
      case ex: ClassNotFoundException => None
    }

  def toInstance[T](cls: Class[T]): Option[T] =
    try {
      Some(cls.newInstance)
    } 
    catch {
      case _ => None
    }
}
