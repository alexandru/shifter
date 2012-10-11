package com.bionicspirit.shifter

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TestSuite extends FunSuite {
  test("1 + 1") {
    assert(1 + 1 === 2)
  }
}
