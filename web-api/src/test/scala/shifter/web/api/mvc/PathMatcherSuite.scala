package shifter.web.api.mvc

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PathMatcherSuite extends FunSuite {
  test("/") {
    "/" match {
      case p"/" =>
        assert(condition = true)
      case _ =>
        assert(condition = false)
    }
  }

  test("/hello/${name}") {
    val p"/hello/$name1" = "/hello/alex"
    assert(name1 === "alex")

    val p"/hello/$name2/" = "/hello/alex"
    assert(name2 === "alex")

    val p"/hello/$name3/" = "/hello/alex/"
    assert(name3 === "alex")
  }

  test("/hello/${name}/something/${age}") {
    val p"/hello/$name1/something/$age1" = "/hello/alex/something/30"
    assert(name1 === "alex")
    assert(age1  === "30")

    val p"/hello/$name2/something/$age2" = "/hello/amy/something/28"
    assert(name2 === "amy")
    assert(age2  === "28")

    val p"/hello/$name3{\w+}/something/$age3{\d+}" = "/hello/cristian/something/2"
    assert(name3 === "cristian")
    assert(age3  === "2")
  }

  test("capture whole path") {
    try {
      val p"$path{.*}" = "/hello/world/something/"
      assert(condition = false)
    }
    catch {
      case _: IllegalArgumentException =>
    }

    val p"/$path{.*}" = "/hello/world/something/"
    assert(path === "hello/world/something/")

    val p"/$path2{.*?}/" = "/hello/world/something/"
    assert(path2 === "hello/world/something")

    val p"/$path3{.*?}/" = "/hello/world/something"
    assert(path3 === "hello/world/something")

    val p"/$path4{.*}" = "/hello/world/something"
    assert(path4 === "hello/world/something")
  }

  test("illegal expressions") {
    try {
      val p"/$path{(hello)}" = "/hello"
      assert(condition = false)
    }
    catch {
      case _: IllegalArgumentException =>
    }

    try {
      val p"$path{.*}" = "/hello"
      assert(condition = false)
    }
    catch {
      case _: IllegalArgumentException =>
    }

    try {
      val p"$hello/()" = "/hello"
      assert(condition = false)
    }
    catch {
      case _: IllegalArgumentException =>
    }

    try {
      val p"$path{.+?}()()" = "/hello"
      assert(condition = false)
    }
    catch {
      case _: IllegalArgumentException =>
    }

    try {
      val p"$hello/{}" = "/hello"
      assert(condition = false)
    }
    catch {
      case _: IllegalArgumentException =>
    }
  }

  test("simple matches") {
    val result1 = "/" match { case p"/" => "/"; case _ => "no match" }
    assert(result1 === "/")

    val result2 = "/hello/world" match { case p"/hello/world" => "/hello/world"; case _ => "no match" }
    assert(result2 === "/hello/world")

    val result3 = "/hello/world" match { case p"/hello/world/" => "/hello/world/"; case _ => "no match" }
    assert(result3 === "/hello/world/")

    val result4 = "/hello/world/" match { case p"/hello/world/" => "/hello/world/"; case _ => "no match" }
    assert(result4 === "/hello/world/")
  }
}
