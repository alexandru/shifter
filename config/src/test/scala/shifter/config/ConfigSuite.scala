package shifter.config

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ConfigSuite extends FunSuite {
  test("config load from string") {
    val configYaml = """---
    host: localhost
    port: 9000
    logging:
      format: something
    funkyarray:
      - 1
      - some string
      - 2
    stringarray:
      - hello world
    intarray:
      - 1
      - 2
      - 3
    """

    val config = load(configYaml, None)

    assert(config("host") === "localhost")
    assert(config("port") === "9000")
    assert(config("funkyarray.1") === "some string")
    assert(config("funkyarray.2") === "2")
    assert(config("stringarray.0") === "hello world")
  }

  test("load settings from resource") {
    val config = loadResource("/shifter/config/sample.yml", None)

    assert(config("http.host") === "127.0.0.2")
    assert(config("http.port") === "9999")
    assert(config("http.maxThreads") === "120")
    assert(config("logging.format") === "%-5p [%d{yyyy-MM-dd HH:mm:ss}] %c{1}: %m\n")
    assert(config("logging.level") === "DEBUG")
  }

  test("override settings manually") {
    val settings = Map(
      "http.host" -> "127.0.0.3",
      "http.port" -> "9000",
       // null or blank or None values should be ignored
      "config.logging.format" -> null
    )

    val config = loadResource("/shifter/config/sample.yml", Some(settings))

    assert(config("http.host") === "127.0.0.3")
    assert(config("http.port") === "9000")
    assert(config("http.maxThreads") === "120")
    assert(config("logging.format") === "%-5p [%d{yyyy-MM-dd HH:mm:ss}] %c{1}: %m\n")
    assert(config("logging.level") === "DEBUG")
  }

  test("override settings by system env") {
    System.setProperty("http.host", "127.0.0.3")
    System.setProperty("http.port", "9000")
    System.setProperty("logging.level", "ERROR")
    System.setProperty("logging.format", "   ")

    val config = loadSystemConfig(Some("/shifter/config/sample.yml"))

    assert(config("http.host") === "127.0.0.3")
    assert(config("http.port") === "9000")
    assert(config("http.maxThreads") === "120")
    assert(config("logging.format") === "%-5p [%d{yyyy-MM-dd HH:mm:ss}] %c{1}: %m\n")
    assert(config("logging.level") === "ERROR")
  }
}
