package shifter.web.api.mvc

import shifter.web.api.responses._
import shifter.web.api.http._
import shifter.web.api.requests._

trait Controller extends ResultBuilders with MimeTypes with Status with HeaderNames {
  val parse = BodyParsers
}
