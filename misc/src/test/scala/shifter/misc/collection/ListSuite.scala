package shifter.misc.collection

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ListSuite extends FunSuite {

  test("list.size()") {
    val list = 1 :: 2 :: 3 :: 4 :: Nil
    assert(list.size === 4)
    assert(list.isEmpty === false)    
    assert(Cons(1, Nil).tail.isEmpty === true)
  }

  test("foreach()") {
    val list = 1 :: 2 :: 3 :: 4 :: Nil
    var sum = 0    
    list.foreach { x => sum += x }
    assert(sum === 10)
  }

  test("reverse()") {
    val list = 1 :: 2 :: 3 :: 4 :: Nil
    val reversed = 4 :: 3 :: 2 :: 1 :: Nil

    assert(list.reverse === reversed)
  }

  test("prepend() / append()") {
    val list = 1 :: 2 :: 3 :: 4 :: Nil    
    assert(list.prepend(0) === (0 :: 1 :: 2 :: 3 :: 4 :: Nil))
    assert(list.append(5)  === (1 :: 2 :: 3 :: 4 :: 5 :: Nil))
  }

  test("foldLeft()") {
    val list = 1 :: 2 :: 3 :: 4 :: Nil    
    val sum = list.foldLeft(0)(_+_)
    assert(sum === 10)
  }

}
