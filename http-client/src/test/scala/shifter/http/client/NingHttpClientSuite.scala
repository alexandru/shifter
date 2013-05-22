package shifter.http.client

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import shifter.concurrency.extensions._
import concurrent.duration._

@RunWith(classOf[JUnitRunner])
class NingHttpClientSuite extends FunSuite {
  test("simple get") {
    val client = NingHttpClient()

    try {
      val result = client.request("GET", "http://icanhazip.com/?hello=world").await(1.minute)
      assert(result.status === 200)
      assert(result.bodyAsString.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+\\s*$"))
    }
    finally {
      client.close()
    }
  }
}
