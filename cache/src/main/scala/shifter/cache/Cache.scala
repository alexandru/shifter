package shifter.cache

import concurrent.{ExecutionContext, Future}

trait Cache {
  def add(key: String, value: Any, exp: Int = 10 * 60): Boolean
  def fireAdd(key: String, value: Any, exp: Int = 10 * 60)(implicit ec: ExecutionContext)

  def asyncAdd(key: String, value: Any, exp: Int = 10 * 60)(implicit ec: ExecutionContext): Future[Boolean]
  def asyncCAS[T](key: String, expecting: Option[T], newValue: T, exp: Int = 60)(implicit ec: ExecutionContext): Future[Boolean]

  def asyncTransformAndGet[T](key: String, exp: Int = 60)(cb: Option[T] => T)(implicit ec: ExecutionContext): Future[T]

  def set(key: String, value: Any, exp: Int = 10 * 60): Boolean
  def fireSet(key: String, value: Any, exp: Int = 10 * 60)(implicit ec: ExecutionContext)

  def get[A](key: String): Option[A]
  def getAsync[A](key: String)(implicit ec: ExecutionContext): Future[A]
  def getAsyncOpt[A](key: String)(implicit ec: ExecutionContext): Future[Option[A]]

  def getBulk(keys: Seq[String]): Map[String, Any]
  def getAsyncBulk(keys: Seq[String])(implicit ec: ExecutionContext): Future[Map[String, Any]]

  def shutdown()
}








