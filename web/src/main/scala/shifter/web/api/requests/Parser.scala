package shifter.web.api.requests

trait Parser {
  val json = JsonParser
  val jsonLazy = new LazyParser(json)

  val form = FormParser
  val formLazy = new LazyParser(form)

  val multiPart = MultiPartParser
  val multiPartLazy = new LazyParser(multiPart)

  val mixed = MixedParser
  val mixedLazy = new LazyParser(mixed)

  val simple = SimpleParser
  val simpleLazy = new LazyParser(simple)

  val raw = RawParser
}

object Parser extends Parser
