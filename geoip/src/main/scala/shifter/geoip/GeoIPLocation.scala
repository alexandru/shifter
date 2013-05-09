package shifter.geoip

case class GeoIPLocation(
  countryCode: String,
  countryCode3: String,
  city: Option[String],
  countryName: Option[String],
  latitude: Option[Float],
  longitude: Option[Float],
  areaCode: Option[Int],
  postalCode: Option[String],
  region: Option[String],
  dmaCode: Option[Int]
)