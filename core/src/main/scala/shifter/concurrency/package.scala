package shifter

import concurrent.{Promise, Future}

package object concurrency {
  implicit class FutureExtensionsImplicit[A](val future: Future[A])
    extends AnyVal with FutureExtensions[A]

  implicit class PromiseExtensionsImplicit[A](val promise: Promise[A])
    extends AnyVal with PromiseExtensions[A]
}
