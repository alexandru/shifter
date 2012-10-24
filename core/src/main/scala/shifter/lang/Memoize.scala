package shifter.lang


object Memoize {
  def apply[T: Manifest](group: Any, args: Any*)(process: => T): T = {    
    val fullKey = composeKey(group, manifest[T], args)

    if (map.contains(fullKey))
      map(fullKey).asInstanceOf[T]

    else 
      lock.synchronized {
        // we can have a race condition here, so if the key is already
        // present when the lock is acquired, then do nothing else        
        if (map.contains(fullKey))
          map(fullKey).asInstanceOf[T]
	else {
          val value = process
          map += (fullKey -> value)
          value
	}
      }
  }

  @volatile
  private[this] var map = Map.empty[Any, Any]
  private[this] val lock = new AnyRef

  private[this] def composeKey[T](group: Any, m: Manifest[T], rest: Any*) =
    if (rest.length == 0)
      (group, m)
    else if (rest.length == 1)
      (group, m, rest(0))
    else if (rest.length == 2)
      (group, m, rest(0), rest(1))
    else if (rest.length == 3)
      (group, m, rest(0), rest(1), rest(2))
    else
      (group, m, rest.toSeq)
}
