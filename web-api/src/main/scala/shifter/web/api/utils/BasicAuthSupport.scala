package shifter.web.api.utils

import org.apache.commons.codec.binary.Base64
import shifter.web.api.requests.HttpRequest
import shifter.web.api.http.HeaderNames._


trait BasicAuthSupport {
  def isAuthenticated(request: HttpRequest[_], user: String, password: String): Boolean = {
    val authorization = request.header(AUTHORIZATION).getOrElse("")
    !authorization.isEmpty && isAuthenticated(authorization, user, password)
  }

  def isAuthenticated(auth: String, user: String, password: String): Boolean =
    if (auth != null && !auth.isEmpty) {
      val parts = auth.split(" ", 2)
      if (parts.length != 2 || parts(0).toLowerCase != "basic")
        false
      else {
        val auth = Base64.decodeBase64(parts(1).trim).map(_.toChar).mkString("")
        val userPass = auth.split(":", 2)
        userPass.length == 2 && userPass(0) == user && userPass(1) == password
      }
    }
    else
      false
}
