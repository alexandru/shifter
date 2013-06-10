package shifter.collection

import scala.collection.BitSet
import scala.annotation.tailrec

@SerialVersionUID(9084149408528910205L)
final case class BloomFilterAny[T](bits: BitSet, numHashF: Int, numBits: Int) extends BloomFilter[T] {
  val strategy = "hashCode"

  def mightContain(elem: T): Boolean = {
    @tailrec
    def loop(hash1: Int, hash2: Int, i: Long): Boolean = {
      if (i <= numHashF) {
        val nh = (hash1 + i * hash2).toInt
        val nextHash = if (nh < 0) ~nh else nh
        if (!bits(nextHash % numBits))
          false
        else
          loop(hash1, hash2, i + 1)
      }
      else
        true
    }

    val hash1 = elem.hashCode()
    val hash2 = hash1 >>> 32
    loop(hash1, hash2, 1)
  }

  def add(elem: T): BloomFilter[T] = {
    @tailrec
    def loop(hash1: Int, hash2: Int, i: Long, acc: BitSet): BitSet = {
      if (i <= numHashF) {
        val nh = (hash1 + i * hash2).toInt
        val nextHash = if (nh < 0) ~nh else nh
        loop(hash1, hash2, i + 1, acc + nextHash % numBits)
      }
      else
        acc
    }

    val hash1 = elem.hashCode()
    val hash2 = hash1 >>> 32
    val newBitSet = loop(hash1, hash2, 1, bits)
    BloomFilterAny(newBitSet, numHashF, numBits)
  }

  def union(other: BloomFilter[T]): BloomFilter[T] =
    if (isCompatible(other))
        BloomFilterAny(bits ++ other.bits, numHashF, numBits)
    else
        throw new IllegalArgumentException("other is not compatible with this BloomFilter")
}