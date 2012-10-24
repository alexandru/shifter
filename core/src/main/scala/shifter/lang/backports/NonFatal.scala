package shifter.lang.backports

/**
 * Backport of NonFatal from Scala 2.10
 * http://www.scala-lang.org/archives/downloads/distrib/files/nightly/docs/library/index.html#scala.util.control.NonFatal$
 */
object NonFatal {
   /**
    * Returns true if the provided `Throwable` is to be considered non-fatal, or false if it is to be considered fatal
    */
   def apply(t: Throwable): Boolean = t match {
     case _: StackOverflowError => true // StackOverflowError ok even though it is a VirtualMachineError
     // VirtualMachineError includes OutOfMemoryError and other fatal errors
     case _: VirtualMachineError | _: ThreadDeath | _: InterruptedException | _: LinkageError => false
     case _ => true
   }
  /**
   * Returns Some(t) if NonFatal(t) == true, otherwise None
   */
  def unapply(t: Throwable): Option[Throwable] = if (apply(t)) Some(t) else None
}
