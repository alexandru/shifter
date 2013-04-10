package shifter.web.api.mvc

import shifter.web.api.responses.ResponseBuilders
import shifter.web.api.requests.Parsers

trait Controller extends ResponseBuilders {
  lazy val parse = Parsers
}
