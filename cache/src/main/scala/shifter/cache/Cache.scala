package shifter.cache

import concurrent.{ExecutionContext, Future}

trait Cache {
  def add(key: String, value: Any, exp: Int = 10 * 60): Boolean
  def fireAdd(key: String, value: Any, exp: Int = 10 * 60)(implicit ec: ExecutionContext)

  def set(key: String, value: Any, exp: Int = 10 * 60): Boolean
  def fireSet(key: String, value: Any, exp: Int = 10 * 60)(implicit ec: ExecutionContext)

  def get[A](key: String): Option[A]
  def getAsync[A](key: String)(implicit ec: ExecutionContext): Future[A]
  def getAsyncOpt[A](key: String)(implicit ec: ExecutionContext): Future[Option[A]]

  def getBulk(keys: Seq[String]): Map[String, Any]
  def getAsyncBulk(keys: Seq[String])(implicit ec: ExecutionContext): Future[Map[String, Any]]

  def shutdown()
}








