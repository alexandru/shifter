package shifter.web.api.requests

import java.io.InputStream
import shifter.concurrency.atomic.Ref
import shifter.web.api.requests.parsers._
import scala.util.Try
import shifter.web.api.http.MultiPartBody

trait AnyContent {
  def request: RequestHeader
  def asForm: Option[Map[String, Seq[String]]]
  def asMixedForm: Option[Map[String, Seq[String]]]
  def asMultiPart: Option[MultiPartBody]
  def asRaw: Option[InputStream]
}

object AnyContent {
  def apply(rh: RequestHeader): AnyContent =
    new Implementation(rh)

  private[this] class Implementation(val request: RequestHeader) extends AnyContent {
    lazy val asForm = {
      checkProcessed("form")
      Try(FormParser(request).right.toOption).toOption.flatten
    }

    lazy val asMixedForm = {
      checkProcessed("mixed form")
      Try(MixedFormParser(request).right.toOption).toOption.flatten
    }

    lazy val asMultiPart = {
      checkProcessed("multi-part")
      Try(MultiPartParser(request).right.toOption).toOption.flatten
    }

    lazy val asRaw = {
      checkProcessed("raw")
      Try(RawParser(request).right.toOption).toOption.flatten
    }

    private[this] def checkProcessed(name: String) {
      if (!isParsed.compareAndSet(null, name))
        throw new IllegalStateException("Request was already parsed as %s".format(isParsed.get))
    }

    private[this] val isParsed = Ref(null : String)
  }
}
