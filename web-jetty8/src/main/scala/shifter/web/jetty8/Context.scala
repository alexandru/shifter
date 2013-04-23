package shifter.web.jetty8

import javax.servlet.{Filter, Servlet}

case class Context(
  servlets: List[ServletConfig] = List.empty,
  filters: List[FilterConfig] = List.empty,
  data: Map[String, Any] = Map.empty
)

case class FilterConfig(
  name: String,
  instance: Filter,
  urlPattern: String
)

case class ServletConfig(
  name: String,
  instance: Servlet,
  urlPattern: String
)