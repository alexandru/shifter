package shifter.cache

import concurrent.Future

trait Cache {
  def add(key: String, value: Any, exp: Int = 10 * 60): Boolean
  def fireAdd(key: String, value: Any, exp: Int = 10 * 60)

  def set(key: String, value: Any, exp: Int = 10 * 60): Boolean
  def fireSet(key: String, value: Any, exp: Int = 10 * 60)

  def get[A](key: String): Option[A]
  def getAsync[A](key: String): Future[A]
  def getAsyncOpt[A](key: String): Future[Option[A]]

  def getBulk(keys: Seq[String]): Map[String, Any]
  def getAsyncBulk(keys: Seq[String]): Future[Map[String, Any]]

  def shutdown()
}








