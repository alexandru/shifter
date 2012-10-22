package shifter.logging

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.io._

@RunWith(classOf[JUnitRunner])
class ConsoleLoggingSuite extends FunSuite {
  test("do some simple logging to System.out") {
    Logger.configure {
      c => c.level = Level.WARN
    }

    val oldOut = System.out

    try {
      val baos = new ByteArrayOutputStream()
      val ps = new PrintStream(baos)
      System.setOut(ps)
      
      val log = Logger("TestLogger")
      log.info("MESSAGE INFO LINE")
      log.warn("MESSAGE WARN LINE")

      val output = baos.toString.trim
      assert(output.contains("MESSAGE WARN LINE"))
      assert(!output.contains("MESSAGE INFO LINE"))
    }
    finally {
      Logger.configureWithDefaults
      System.setOut(oldOut)
    }
  }
}
