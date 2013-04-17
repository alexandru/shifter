package shifter.web.api.mvc

import shifter.web.api.responses.ResponseBuilders
import shifter.web.api.http.{HeaderNames, Status, MimeTypes}
import shifter.web.api.requests.Parser

trait Controller extends ResponseBuilders with MimeTypes with Status with HeaderNames {
  val parser = Parser
}
