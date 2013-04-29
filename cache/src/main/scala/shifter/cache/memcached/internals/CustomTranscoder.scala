package shifter.cache.memcached.internals

import net.spy.memcached.transcoders.SerializingTranscoder
import java.io.{ObjectStreamClass, ObjectInputStream, ByteArrayInputStream}
import net.spy.memcached.compat.CloseUtil

class CustomTranscoder extends SerializingTranscoder {
  override def deserialize(in: Array[Byte]): AnyRef = {
    val classLoader = Thread.currentThread().getContextClassLoader
    var bis: ByteArrayInputStream = null
    var is: ObjectInputStream = null

    try {
      if (in != null) {
        bis = new ByteArrayInputStream(in)
        is = new ObjectInputStream(bis) {
          override def resolveClass(desc: ObjectStreamClass): Class[_] = {
            try
              classLoader.loadClass(desc.getName)
            catch {
              case ex: Exception =>
                // try fallback to super implementation
                super.resolveClass(desc)
            }
          }
        }

        is.readObject
      }
      else
        null
    }
    finally {
      CloseUtil.close(is)
      CloseUtil.close(bis)
    }
  }
}
