package shifter

import shifter.lang._
import java.io.{File, FileInputStream}
import java.util.zip.ZipInputStream
import collection.JavaConverters._
import annotation.tailrec
import scala.reflect.Manifest


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

  def castTo[T : Manifest](value: Any): Option[T] = {
    import scala.runtime._
    import collection.immutable.StringOps
    val m = manifest[T]

    val (erasure, wantsNumber) = m match {
      case Manifest.Byte => (classOf[java.lang.Byte], true)
      case Manifest.Short => (classOf[java.lang.Short], true)
      case Manifest.Char => (classOf[java.lang.Character], true)
      case Manifest.Long => (classOf[java.lang.Long], true)
      case Manifest.Float => (classOf[java.lang.Float], true)
      case Manifest.Double => (classOf[java.lang.Double], true)
      case Manifest.Boolean => (classOf[java.lang.Boolean], true)
      case Manifest.Int => (classOf[java.lang.Integer], true)	
      case m => (m.erasure, false)
    }

    if(erasure.isInstance(value)) 
      Some(value.asInstanceOf[T]) 

    else if (value.isInstanceOf[String] && wantsNumber) 
      value match {
	case NumberFormat(_*) =>
	  val str = value.asInstanceOf[String]
	  val proxy = new StringOps(str)
	  Some((m match {
	    case Manifest.Byte => proxy.toByte
	    case Manifest.Short => proxy.toShort
	    case Manifest.Char => proxy.toInt.toChar
	    case Manifest.Long => proxy.toLong
	    case Manifest.Float => proxy.toFloat
	    case Manifest.Double => proxy.toDouble
	    case Manifest.Boolean => (if (proxy.toInt != 0) true else false)
            case Manifest.Int => proxy.toInt
	  }).asInstanceOf[T])

	case FloatFormat(_*) =>
	  val str = value.asInstanceOf[String]
	  val proxy = new StringOps(str)
	  Some((m match {
	    case Manifest.Byte => proxy.toDouble.toByte
	    case Manifest.Short => proxy.toDouble.toShort
	    case Manifest.Char => proxy.toDouble.toChar
	    case Manifest.Long => proxy.toDouble.toLong
	    case Manifest.Float => proxy.toFloat
	    case Manifest.Double => proxy.toDouble
	    case Manifest.Boolean => (if (proxy.toDouble != 0) true else false)
            case Manifest.Int => proxy.toDouble.toInt
	  }).asInstanceOf[T])
	  
	case _ => None
      }
    else if (wantsNumber) {
      val proxy = value match {
	case v : Byte => new RichByte(v)
	case v : Short => new RichShort(v)
	case v : Char => new RichChar(v)
	case v : Long => new RichLong(v)
	case v : Float => new RichFloat(v)
	case v : Double => new RichDouble(v)
	case v : Boolean => new RichInt(if (v) 1 else 0)
	case v : Int => new RichInt(v)
	case _ => null
      }

      if (proxy != null)
	Some((m match {
	  case Manifest.Byte => proxy.toByte
	  case Manifest.Short => proxy.toShort
	  case Manifest.Char => proxy.toChar
	  case Manifest.Long => proxy.toLong
	  case Manifest.Float => proxy.toFloat
	  case Manifest.Double => proxy.toDouble
	  case Manifest.Boolean => (if (proxy.toInt != 0) true else false)
          case Manifest.Int => proxy.toInt
	}).asInstanceOf[T])
      else
	None
    }
    else 
      None
  }

  private[this] val FloatFormat = """^[-+]?[0-9]*\.?[0-9]+([eE][-+]?[0-9]+)?$""".r
  private[this] val NumberFormat = """^[-+]?[0-9]+$""".r
}
