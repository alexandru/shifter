package shifter.collection

import scala.collection.BitSet
import scala.annotation.tailrec

trait BloomFilter[T] extends ((T) => Boolean) {
  def mightContain(elem: T): Boolean
  def add(elem: T): BloomFilter[T]
  def union(other: BloomFilter[T]): BloomFilter[T]

  def bits: BitSet
  def strategy: String
  def numHashF: Int
  def numBits: Int

  final def apply(elem: T): Boolean =
    mightContain(elem)

  def +(elem: T): BloomFilter[T] =
    add(elem)

  def ++(other: BloomFilter[T]): BloomFilter[T] =
    union(other)

  def ++(seq: Traversable[T]): BloomFilter[T] = {
    @tailrec
    def loop(elems: Traversable[T], acc: BloomFilter[T]): BloomFilter[T] =
      if (elems.isEmpty)
        acc
      else {
        val head = elems.head
        val tail = elems.tail
        val newAcc = acc + head
        loop(tail, newAcc)
      }

    loop(seq, this)
  }

  def isCompatible(other: BloomFilter[T]): Boolean =
    other != null && numHashF == other.numHashF && numBits == other.numBits

  override def toString(): String =
    s"BloomFilter(hashesNr=$numHashF, bitSetSize=$numBits, strategy=$strategy)"
}

object BloomFilter {
  def optimalNumOfBits(expectedInserts: Long, falsePositivesRate: Double): Int = {
    require(expectedInserts > 0, "expectedInserts must be strictly positive")
    require(falsePositivesRate >= 0 && falsePositivesRate <= 1, "falsePositivesRate must be between 0 and 1")

    val p = if (falsePositivesRate == 0)
      Double.MinValue
    else
      falsePositivesRate

    val optimal = (-expectedInserts * math.log(p) / (math.log(2) * math.log(2))).toLong
    if (optimal > Int.MaxValue)
      Int.MaxValue
    else
      optimal.toInt
  }

  def optimalNumOfHashFunctions(expectedInserts: Long, numBits: Int): Int = {
    require(expectedInserts > 0, "expectedInserts must be strictly positive")
    require(numBits > 0, "numBits must be strictly positive")

    val optimal = math.max(1, math.round(numBits / expectedInserts * math.log(2)))
    if (optimal > Int.MaxValue)
      Int.MaxValue
    else
      optimal.toInt
  }

  def empty[T](expectedInserts: Long, falsePositivesRate: Double): BloomFilterAny[T] = {
    val numBits = optimalNumOfBits(expectedInserts, falsePositivesRate)
    val numHashF = optimalNumOfHashFunctions(expectedInserts, numBits)
    BloomFilterAny[T](BitSet.empty, numHashF, numBits)
  }

  def empty[T](implicit ev: BloomFilterStrategy[T]): BloomFilter[T] =
    ev.empty

  def apply[T](elems: T*)(implicit ev: BloomFilterStrategy[T]): BloomFilter[T] =
    ev.empty ++ elems
}
