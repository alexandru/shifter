package shifter.collection

import language.existentials
import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.io.{ByteArrayInputStream, ObjectInputStream, ByteArrayOutputStream, ObjectOutputStream}

@RunWith(classOf[JUnitRunner])
class BloomFilterSuite extends FunSuite {

  test("simple") {
    val bloom = BloomFilter("hello", "world", "a", "b", "c")

    assert(bloom("hello"), "'hello' should be contained")
    assert(bloom("world"), "'world' should be contained")
    assert(bloom("a"), "'a' should be contained")
    assert(bloom("b"), "'b' should be contained")
    assert(bloom("c"), "'c' should be contained")

    assert(bloom("alex") === false)
    assert(bloom("d") === false)
  }

  test("concatenation") {
    val bloom1 = BloomFilter("hello", "world")
    val bloom2 = BloomFilter("a", "b", "c")
    val bloom = bloom1 ++ bloom2

    assert(bloom("hello"), "'hello' should be contained")
    assert(bloom("world"), "'world' should be contained")
    assert(bloom("a"), "'a' should be contained")
    assert(bloom("b"), "'b' should be contained")
    assert(bloom("c"), "'c' should be contained")

    assert(bloom("alex") === false)
    assert(bloom("d") === false)
  }

  test("error rate") {
    val number = 10000

    implicit def strategy[T] = new BloomFilterStrategy[T] {
      def expectedInserts: Long = number

      def falsePositivesRate: Double = 0.1

      def empty: BloomFilter[T] =
        BloomFilter.empty[T](expectedInserts, falsePositivesRate)
    }

    val (bloom, seq) = (0 until number).foldLeft((BloomFilter.empty[Long], Vector.empty[Long])) {
      (acc, elem) =>
        val (b, s) = acc
        (b + elem.toLong, s :+ elem.toLong)
    }

    for (uuid <- seq)
      assert(bloom(uuid), s"Key $uuid should be contained")

    var errors = 0

    for (idx <- number until number * 2) {
      if (bloom(idx) && !seq.contains(idx))
        errors += 1
    }

    val chosenStrategy = implicitly[BloomFilterStrategy[String]]
    assert(errors.toDouble / number <= chosenStrategy.falsePositivesRate,
      s"error rate was higher than ${chosenStrategy.falsePositivesRate}: $errors false positives in $number tests")
  }

  test("serialization") {
    val bloom = BloomFilter("hello", "world", "alex")

    val bytes = new ByteArrayOutputStream()
    val out = new ObjectOutputStream(bytes)
    out.writeObject(bloom)

    val bytesArr = bytes.toByteArray
    val in = new ObjectInputStream(new ByteArrayInputStream(bytesArr))
    val obj = in.readObject()

    assert(obj.isInstanceOf[BloomFilter[_]])
    val bloom2 = obj.asInstanceOf[BloomFilter[String]]

    assert(bloom === bloom2)

    assert(bloom2("hello"), "'hello' should be contained")
    assert(bloom2("world"), "'world' should be contained")
    assert(bloom2("alex"),  "'alex' should be contained")
    assert(!bloom2("unknown"),  "'unknown' should not be contained")
  }
}
