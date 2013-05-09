package shifter.web.api2.http

import shifter.geoip.GeoIPLocation

case class UserInfo(
  ip: String,
  forwardedFor: String,
  via: String,
  agent: String,
  geoip: Option[GeoIPLocation]
)