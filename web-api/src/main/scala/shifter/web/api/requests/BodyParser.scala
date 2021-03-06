package shifter.web.api.requests

import shifter.web.api.responses.Result
import shifter.web.api.requests.parsers._

trait BodyParser[+A] extends (RequestHeader => Either[Result, A]) {
  self =>

  def map[B](f: A => B): BodyParser[B] = new BodyParser[B] {
    def apply(request: RequestHeader): Either[Result, B] =
      self(request).right.map(f)
  }

  def flatMap[B](f: A => BodyParser[B]): BodyParser[B] = new BodyParser[B] {
    def apply(request: RequestHeader): Either[Result, B] =
      self(request) match {
        case Left(e) => Left(e)
        case Right(a) => f(a)(request)
      }
  }
}

trait BodyParsers {
  val string = StringParser
  val form = FormParser
  val mixedForm = MixedFormParser
  val multiPart = MultiPartParser
  val raw = RawParser
  val anyContent = AnyContentParser
}

object BodyParsers extends BodyParsers


