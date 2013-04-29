package shifter.models

case class UserInfo(
  ip: String,
  forwardedFor: String,
  via: String,
  agent: String,
  geoip: Option[GeoIPLocation]
)