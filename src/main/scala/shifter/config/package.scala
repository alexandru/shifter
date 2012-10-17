package shifter

import org.yaml.snakeyaml.Yaml
import collection.JavaConverters._
import scala.collection.JavaConversions.propertiesAsScalaMap
import java.io.{FileNotFoundException, BufferedReader, InputStreamReader, InputStream}
import java.util.{HashMap, Map => JavaMap}
import java.lang.{Iterable => JavaIterable}


/**
 * Author: Alexandru Nedelcu
 * Email:  contact@alexn.org
 */

package object config {
  
  /**
   * Loads the configuration specified in a YAML file and overrides the parameters from the
   * system environment.
   *
   * <pre>
   *   {@code
   *   System.setProperty("logging.format", "%-5p %d{yyyy-MM-dd HH:mm:ss} %c{1}: %m\n")
   *
   *   val config = loadSystemConfig("/path/to/resource.yml")
   *   assert(config.logging.format == "%-5p %d{yyyy-MM-dd HH:mm:ss} %c{1}: %m\n")
   *   }
   * </pre>
   *
   * @param path the resource path to the YAML configuration file
   * @return an instance of Map[String, String] initialized with the specified parameters
   */
  def loadSystemConfig(path: Option[String]): Map[String, String] =
    path match {
      case Some(p) => loadResource(p, Option(System.getProperties.toMap))
      case None =>
	try {
	  loadSystemConfig(Some("/config/local.yml"))
	} catch {
	  case e: FileNotFoundException =>
            loadSystemConfig(Some("/config/main.yml"))
	}
    }
	  

  /**
   * Loads the configuration specified in a YAML formatted file. It allows for overriding of
   * those parameters by taking a second argument representing a map of values.
   *
   * <pre>
   *   {@code
   *   val withValues = Map(
   *     "logging.format" -> "%-5p %d{yyyy-MM-dd HH:mm:ss} %c{1}: %m\n",
   *     "database.url"   -> "jdbc:postgresql://db.example.com/db-prod"
   *   )
   *
   *   val config = loadResource("/WEB-INF/development.yml", withValues)
   *   }
   * </pre>
   *
   * @param path    the resource path to the YAML configuration file.
   * @param system  the map of values used for overriding the values defined in the
   *                YAML configuration file
   * @return an instance of an initialized configuration object
   */
  def loadResource(path: String, system: Option[Map[String, String]]): Map[String, String] = {
    val resource = getClass.getResource(path)
    if (resource == null) throw new FileNotFoundException(path)
    load(resource.openStream(), system)
  }

  /**
   * Loads the configuration specified in a YAML formatted file. It allows for overriding of
   * those parameters by taking a second argument representing a map of values. It behaves as
   * loadResource, but takes an arbitrary InputStream to a YAML file.
   *
   * @param stream  an input stream, representing the YAML file to be loaded
   * @param system  the map of values used for overriding the values defined in the
   *                YAML configuration file
   * @return an instance of an initialized configuration object
   */
  def load(stream: InputStream, system: Option[Map[String, String]]): Map[String, String] = {
    val reader = new BufferedReader(new InputStreamReader(stream, "utf-8"))
    val text = new StringBuilder
    var hasLines = true

    while (hasLines) {
      val line = reader.readLine()
      hasLines = line != null
      if (hasLines) text.append(line).append('\n')
    }

    load(text.toString(), system)
  }

  /**
   * Loads the configuration specified in a YAML formatted file. It allows for overriding of
   * those parameters by taking a second argument representing a map of values. It behaves as
   * loadResource, but takes an arbitrary String representing the YAML file.
   *
   * @param text    a string, representing the YAML file to be loaded
   * @param system  the map of values used for overriding the values defined in the
   *                YAML configuration file
   * @return an instance of an initialized config
   */
  def load(text: String, system: Option[Map[String, String]]): Map[String, String] = {
    val configProps = if (text != null && !text.isEmpty)
      yamlMap.load(text).asInstanceOf[JavaMap[String, Object]]
    else
      new HashMap[String, Object]()

    if (system != None)
      overrideFromMap(configProps, system.get, List())

    asImmutableMap(configProps)
  }

  private[this] def asImmutableMap(props: JavaMap[String, Object]) = {
    def iter(props: JavaMap[String, Object], path: String): Map[String, String] = {
      var map = Map.empty[String, String]
      
      for (key <- props.keySet.asScala) {
	val value = props.get(key)
	val newPath = if (path.isEmpty) key else path + "." + key

	val newValues = value match {
	  case jmap : JavaMap[_, _] =>
	    iter(jmap.asInstanceOf[JavaMap[String, Object]], newPath)
	  case jiter : JavaIterable[_] =>
	    val iter = jiter.asInstanceOf[JavaIterable[Object]].asScala
	    val imap = Map.empty[String, String]

	    val values = (0 until iter.size).zip(iter).map {
	      case (idx, v) => (newPath + "." + idx, v.toString)
	    }
		
	    values.toMap
	  case x if x != null =>
	    Map(newPath -> value.toString)
	  case _ =>
	    Map.empty[String, String]
	}

	map = map ++ newValues
      }      
      map 
    }

    iter(props, "")
  }

  private[this] def overrideFromMap(configProps: JavaMap[String, Object],
                                    system: Map[String, String],
                                    components: List[String]) {

    val iter = configProps.keySet().iterator()

    def valueIsValid(x: Any) =
      x != null && !(x.isInstanceOf[String] && x.asInstanceOf[String].trim.isEmpty) && x != None

    while (iter.hasNext) {
      val key = iter.next()
      val prop = components :+ key

      configProps.get(key) match {
        case m: JavaMap[_, _] => overrideFromMap(m.asInstanceOf[JavaMap[String, Object]], system, prop)
        case x => {
          val propStr = prop.reduceLeft(_ + "." + _)
          if (system.contains(propStr) && valueIsValid(system(propStr)))
            configProps.put(key, system(propStr))
        }
      }
    }
  }

  private[this] lazy val yamlMap = {
    new Yaml()
  }
}
