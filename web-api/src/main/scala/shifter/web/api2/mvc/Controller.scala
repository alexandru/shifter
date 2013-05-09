package shifter.web.api2.mvc

import shifter.web.api2.responses._
import shifter.web.api2.http._
import shifter.web.api2.requests._

trait Controller extends ResultBuilders with MimeTypes with Status with HeaderNames {
  val parse = BodyParsers
}
