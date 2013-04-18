package shifter.web.api.requests

import javax.servlet.http.HttpServletRequest

object RawParser extends RequestParser[HttpServletRequest, RawRequest] {
  def canBeParsed(raw: RawRequest): Boolean = true

  def parse(raw: RawRequest): Option[RawRequest] =
    Some(raw)
}
