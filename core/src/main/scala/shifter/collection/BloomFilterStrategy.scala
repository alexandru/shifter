package shifter.collection

import scala.collection.BitSet


trait BloomFilterStrategy[T] {
  def expectedInserts: Long
  def falsePositivesRate: Double
  def empty: BloomFilter[T]
}

object BloomFilterStrategy {
  implicit def defaultStrategy[T] = new BloomFilterStrategy[T] {
    val expectedInserts: Long = 100000000L
    val falsePositivesRate: Double = 0.1

    def empty: BloomFilter[T] = {
      import BloomFilter._
      val numBits = optimalNumOfBits(expectedInserts, falsePositivesRate)
      val numHashF = optimalNumOfHashFunctions(expectedInserts, numBits)
      BloomFilterAny(BitSet.empty, numHashF, numBits)
    }
  }
}
