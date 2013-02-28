package shifter

import scala.language.reflectiveCalls

package object lang {

  def memoize[T: Manifest](group: Any, args: Any*)(process: => T): T =
    Memoize(group, args)(process)

  type Closeable = { def close() }

  def using[A, B <: Closeable](closable: B)(f: B => A): A =
    try {
      f(closable)
    }
    finally {
      closable.close()
    }
}
