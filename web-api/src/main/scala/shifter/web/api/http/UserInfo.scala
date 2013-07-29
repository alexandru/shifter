package shifter.web.api.http

case class UserInfo(
  ip: String,
  forwardedFor: String,
  via: String,
  agent: String
)