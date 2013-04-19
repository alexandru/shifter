package shifter.utils

object glob {
  def decompose(pattern: String, string: String): Vector[String] = {
    require(pattern != null && pattern.length > 0, "the pattern must not be empty")

    val buf = allocateBuffer(pattern, string)

    //val header1 = "    " + (0 until string.length).map(nr => "%3d".format(nr)).mkString("")
    //println(header1)
    //println("-" * header1.length)

    //val header = "    " + string.map(ch => "%3s".format(ch)).mkString("")
    //println(header)
    //println("-" * header.length)

    var pIdx = 0
    var sIdx = 0
    var isMatch = true

    while (isMatch && pIdx < pattern.length) {
      //print("%s | ".format(pattern.charAt(pIdx)))

      sIdx = 0
      isMatch = false

      while (sIdx < string.length) {
        val currentIdx = (pIdx * string.length + sIdx) * 2

        val leftIdx = if (sIdx > 0)
          (pIdx * string.length + (sIdx - 1)) * 2
        else
          -1

        val diagonalIdx = if (sIdx > 0 && pIdx > 0)
          ((pIdx - 1) * string.length + (sIdx - 1)) * 2
        else
          -1

        val pChar = pattern.charAt(pIdx)

        if (pChar == string.charAt(sIdx))
          if (pIdx == 0 && sIdx == 0) {
            buf(0) = 0
            buf(1) = -1
            isMatch = true
          }
          else if (diagonalIdx >= 0 && buf(diagonalIdx) >= 0)
            // if previous char was a wildcard, change index
            if (pIdx - 1 >= 0 && pattern.charAt(pIdx - 1) == '*') {
              buf(currentIdx) = sIdx
              buf(currentIdx + 1) = diagonalIdx
              isMatch = true
            }
            else {
              buf(currentIdx) = buf(diagonalIdx)
              buf(currentIdx + 1) = diagonalIdx
              isMatch = true
            }
          else {
            buf(currentIdx) = -1
            buf(currentIdx + 1) = -1
          }

        else if (pChar == '*')
          if (pIdx == 0 && sIdx == 0) {
            buf(0) = 0
            buf(1) = -1
            isMatch = true
          }
          else if (diagonalIdx >= 0 && buf(diagonalIdx) >= 0) {
            buf(currentIdx) = sIdx
            buf(currentIdx + 1) = diagonalIdx
            isMatch = true
          }
          else if (leftIdx >= 0 && buf(leftIdx) >= 0) {
            buf(currentIdx) = buf(leftIdx)
            buf(currentIdx + 1) = leftIdx
            isMatch = true
          }
          else {
            buf(currentIdx) = -1
            buf(currentIdx + 1) = -1
          }
        else {
          buf(currentIdx) = -1
          buf(currentIdx + 1) = -1
        }

        //print("%3d".format(buf(currentIdx)))

        sIdx += 1
      }

      //println()
      pIdx += 1
    }

    if (isMatch) {
      pIdx = pattern.length - 1
      sIdx = string.length - 1

      var currentIdx = (pIdx * string.length + sIdx) * 2

      var parts = Vector.empty[String]
      var startSIdx = -1
      var endSIdx = -1

      //println()
      while (currentIdx > -1) {
        val value = buf(currentIdx)
        val nextIdx = buf(currentIdx + 1)

        if (startSIdx == -1) {
          startSIdx = value
          endSIdx = currentIdx / 2 % string.length
        }
        else if (startSIdx != value) {
          parts = string.substring(startSIdx, endSIdx + 1) +: parts
          startSIdx = value
          endSIdx = currentIdx / 2 % string.length
        }

        //print("%d ".format(buf(currentIdx)))
        currentIdx = nextIdx
      }

      //println()

      assert(currentIdx == -1 && startSIdx == 0 && endSIdx >= 0)
      parts = string.substring(0, endSIdx + 1) +: parts

      parts
    }
    else
      Vector.empty[String]
  }

  private[this] def allocateBuffer(pattern: String, string: String) =
    if (pattern.length <= 50 && string.length <= 100)
      localBuffer.get()
    else
      new Array[Int](pattern.length * string.length * 2)

  private[this] val localBuffer = new ThreadLocal[Array[Int]] {
    override def initialValue(): Array[Int] =
      new Array[Int](50 * 100 * 2)
  }
}

object GlobTest extends App {
  var string = "/hello/12/world/13/shifter"
  val Extractor = """^/hello/([^/]+)/world/([^/]+)/shifter$""".r
  val pattern = "/hello/*/world/*/shifter"

  println(glob.decompose(pattern, string))
  println(Extractor.findFirstIn(string))
  println()

  var iters = 10000000
  var result: AnyRef = null

  def benchmarkGlob() {
    println("Measuring glob ...")
    val start = System.currentTimeMillis()

    var idx = 0
    while (idx < iters) {
      result = glob.decompose(pattern, string)
      idx += 1
    }

    val seconds = (System.currentTimeMillis() - start) / 1000.0
    println("Seconds: %.2f\n".format(seconds))
  }

  def benchmarkRegexp() {
    println("Measuring regular expressions ...")
    val start = System.currentTimeMillis()

    var idx = 0
    while (idx < iters) {
      result = Extractor.findFirstIn(string)
      idx += 1
    }

    val seconds = (System.currentTimeMillis() - start) / 1000.0
    println("Seconds: %.2f\n".format(seconds))
  }

  benchmarkRegexp()
  benchmarkGlob()

  benchmarkRegexp()
  benchmarkGlob()
}

