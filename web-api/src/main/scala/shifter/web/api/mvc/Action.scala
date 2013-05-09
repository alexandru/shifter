package shifter.web.api.mvc

import shifter.web.api.responses.Result
import shifter.web.api.requests._
import shifter.web.api.requests.parsers.StringParser


trait Action[A] extends (RequestHeader => Result) {
  /**
   * Body parser associated with this action.
   */
  def parser: BodyParser[A]

  /**
   * Invokes this action.
   */
  def apply(request: Request[A]): Result

  def apply(rh: RequestHeader): Result = parser(rh) match {
    case Left(result) => result
    case Right(body) =>
      val request = Request(rh, body)
      apply(request)
  }

  override def toString() = {
    "Action(parser=" + parser.getClass.getName + ")"
  }
}

/**
 * Provides helpers for creating `Action` values.
 */
object Action {
  def apply[A](bodyParser: BodyParser[A])(block: Request[A] => Result): Action[A] = new Action[A] {
    def parser = bodyParser

    def apply(request: Request[A]): Result =
      try
        block(request)
      catch {
        // NotImplementedError is not caught by NonFatal, wrap it
        case e: NotImplementedError =>
          throw new RuntimeException(e)
      }
  }

  def apply(block: Request[String] => Result): Action[String] =
    apply(StringParser)(block)

  def apply(block: => Result): Action[String] = new Action[String] {
    def parser: BodyParser[String] = StringParser
    def apply(request: Request[String]): Result = block
    override def apply(rh: RequestHeader): Result = block
  }
}