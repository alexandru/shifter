package shifter

package object units {
  implicit class ByteUnitsInt(val number: Int) extends AnyVal {
    def kilobytes: Int = number * 1024
    def megabytes: Int = number * 1048576
    def gigabytes: Int = number * 1073741824
  }

  implicit class ByteUnitsLong(val number: Long) extends AnyVal {
    def kilobytes: Long = number * 1024
    def megabytes: Long = number * 1048576
    def gigabytes: Long = number * 1073741824
  }
}
