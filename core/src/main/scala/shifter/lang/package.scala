package shifter


package object lang {
  
  type Closeable = { def close():Unit }

  def using[A, B <: Closeable](closable: B)(f: B => A): A = 
    try {
      f(closable)
    }
    finally {
      closable.close()
    }

  def memoize[T: Manifest](group: Any, args: Any*)(process: => T): T =
    Memoize(group, args)(process)
}
