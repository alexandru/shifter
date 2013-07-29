package shifter.db

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import scala.util.Try

@RunWith(classOf[JUnitRunner])
class SqlQuerySuite extends FunSuite {
  test("simple sql query") {
    val db = DB("jdbc:mysql://localhost:3306/shifterdbtest", "root", "")

    db.withConnection { implicit conn =>
      try {
        SQL("CREATE TABLE mytesttable (id INT, name VARCHAR(255), l INT(12))").execute()

        SQL("INSERT INTO mytesttable (id, name, l) VALUES ({id}, {name}, {l})")
          .on("id" -> 1)
          .on("name" -> "Alex")
          .on("l" -> Int.MaxValue)
          .execute()

        SQL("INSERT INTO mytesttable (id, name, l) VALUES ({id}, {name}, {l})")
          .on("id" -> 2)
          .on("name" -> null)
          .on("l" -> Int.MinValue)
          .execute()

        SQL("INSERT INTO mytesttable (id, name, l) VALUES ({id}, {name}, {l})")
          .on("id" -> 3)
          .on("name" -> null)
          .on("l" -> null)
          .execute()

        val Some((id, name, l)) =
          SQL("SELECT * FROM mytesttable WHERE id = {id}")
            .on("id" -> 1)
            .first(r => (
              r[Int]("id"),
              r[Option[String]]("name"),
              r[Option[Long]]("l")
            ))

        assert(id === 1)
        assert(name === Some("Alex"))
        assert(l === Some(Int.MaxValue))

        val Some((id2, name2, l2)) =
          SQL("SELECT * FROM mytesttable WHERE id = {id}")
            .on("id" -> 2)
            .first(r => (
              r[Int]("id"),
              r[Option[String]]("name"),
              r[Option[Long]]("l")
            ))

        assert(id2 === 2)
        assert(name2 === None)
        assert(l2 === Some(Int.MinValue))

        val Some((id3, name3, l3)) =
          SQL("SELECT * FROM mytesttable WHERE id = {id}")
            .on("id" -> 3)
            .first(r => (
              r[Int]("id"),
              r[Option[String]]("name"),
              r[Option[Long]]("l")
            ))

        assert(id3 === 3)
        assert(name3 === None)
        assert(l3 === None)

        try {
          SQL("SELECT * FROM mytesttable WHERE id = {id}")
            .on("id" -> 3)
            .first(r => (
              r[Int]("id"),
              r[Option[String]]("name"),
              r[Long]("l")
            ))

          fail("Should have thrown NullPointerException")
        }
        catch {
          case ex: NullPointerException =>
            // => OK
        }
      }
      finally {
        Try(SQL("DROP TABLE mytesttable").execute())
      }
    }
  }
}