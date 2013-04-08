package shifter

package object units {
  implicit class ByteUnitsInt(val number: Int) extends AnyVal {
    def bytes: Int = number
    def byte: Int = number
    def kilobytes: Int = number * 1024
    def kilobyte: Int = number * 1024
    def megabytes: Int = number * 1048576
    def megabyte: Int = number * 1048576
    def gigabytes: Int = number * 1073741824
    def gigabyte: Int = number * 1073741824
  }

  implicit class ByteUnitsLong(val number: Long) extends AnyVal {
    def bytes: Long = number
    def byte: Long = number
    def kilobytes: Long = number * 1024
    def kilobyte: Long = number * 1024
    def megabytes: Long = number * 1048576
    def megabyte: Long = number * 1048576
    def gigabytes: Long = number * 1073741824
    def gigabyte: Long = number * 1073741824
  }
}
