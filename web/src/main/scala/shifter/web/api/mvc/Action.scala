package shifter.web.api.mvc

import shifter.web.api.requests._
import shifter.web.api.responses.HttpResponse

object Action {
  def apply[T](cb: => HttpResponse[T]): ActionResponse[T] =
    (req: RawRequest) => cb

  def apply[T](cb: SimpleRequest => HttpResponse[T]): ActionResponse[T] =
    (req: RawRequest) => cb(SimpleParser.parse(req).get)

  def apply[T, U <: HttpRequest[T], R](parser: RequestParser[T, U])(cb: HttpRequest[Option[U]] => HttpResponse[R]): ActionResponse[R] =
    (raw: RawRequest) => {
      val request = new LazyRequest[T, U](raw, parser)
      cb(request)
    }
}
