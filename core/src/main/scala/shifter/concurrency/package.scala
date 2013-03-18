package shifter

import concurrent.Future

package object concurrency {
  implicit class FutureExtensionsImplicit[A](val future: Future[A])
    extends AnyVal with FutureExtensions[A]
}
