package shifter.json

import com.fasterxml.jackson.core.{JsonProcessingException, JsonToken, JsonParser, JsonFactory}
import scala.collection.mutable
import java.io.{Reader, InputStream, File}
import java.net.URL

object JsonSax {
  def parse(f: File): Iterator[JsEvent] =
    parse(jsonFactory.createParser(f))

  def parse(content: String): Iterator[JsEvent] =
    parse(jsonFactory.createParser(content))

  def parse(data: Array[Byte], off: Int, len: Int): Iterator[JsEvent] =
    parse(jsonFactory.createParser(data, off, len))

  def parse(data: Array[Byte]): Iterator[JsEvent] =
    parse(jsonFactory.createParser(data))

  def parse(url: URL): Iterator[JsEvent] =
    parse(jsonFactory.createParser(url))

  def parse[T](in: InputStream): Iterator[JsEvent] =
    parse(jsonFactory.createParser(in))

  def parse[T](r: Reader): Iterator[JsEvent] =
    parse(jsonFactory.createParser(r))

  def parse(parser: JsonParser): Iterator[JsEvent] = new Iterator[JsEvent] {
    def next(): JsEvent = {
      def popFieldName() {
        if (!trace.isEmpty && trace.head == JsonToken.FIELD_NAME) {
          trace.pop()
          pathPop()
        }
      }

      def pathPush(elem: Any) = {
        path = path \ elem
        path
      }

      def pathPop() = {
        val (last, rest) = path.pop2
        path = rest
        last
      }

      def ifArrayIncrementIndex() {
        if (!trace.isEmpty && trace.head == JsonToken.START_ARRAY)
          pathPush(pathPop().asInstanceOf[Int] + 1)
      }

      var returnValue: JsEvent = null

      while (returnValue == null && token != null)
        try {
          token = parser.nextToken()

          token match {
            case null =>
            // do nothing

            case JsonToken.START_OBJECT =>
              ifArrayIncrementIndex()
              trace.push(token)

            case JsonToken.END_OBJECT =>
              assert(trace.pop() == JsonToken.START_OBJECT)
              popFieldName()

            case JsonToken.START_ARRAY =>
              ifArrayIncrementIndex()
              trace.push(token)
              pathPush(-1)

            case JsonToken.END_ARRAY =>
              assert(trace.pop() == JsonToken.START_ARRAY)
              assert(pathPop().isInstanceOf[Int])
              popFieldName()

            case JsonToken.FIELD_NAME =>
              assert(trace.head == JsonToken.START_OBJECT)
              trace.push(token)
              pathPush(parser.getText)

            case JsonToken.VALUE_STRING =>
              ifArrayIncrementIndex()
              returnValue = JsString(path, parser.getValueAsString)
              popFieldName()

            case JsonToken.VALUE_FALSE =>
              ifArrayIncrementIndex()
              returnValue = JsBoolean(path, false)
              popFieldName()

            case JsonToken.VALUE_TRUE =>
              ifArrayIncrementIndex()
              returnValue = JsBoolean(path, true)
              popFieldName()

            case JsonToken.VALUE_NUMBER_FLOAT =>
              ifArrayIncrementIndex()
              returnValue = JsDouble(path, parser.getValueAsDouble)
              popFieldName()

            case JsonToken.VALUE_NUMBER_INT =>
              ifArrayIncrementIndex()
              returnValue = JsLong(path, parser.getValueAsLong)
              popFieldName()

            case JsonToken.VALUE_NULL =>
              popFieldName()

            case JsonToken.VALUE_EMBEDDED_OBJECT =>
              popFieldName()

            case JsonToken.NOT_AVAILABLE =>
              popFieldName()
          }
        }
        catch {
          case ex: JsonProcessingException =>
            ifArrayIncrementIndex()
            returnValue = JsError(path, ex.getMessage)
            reachedEnd = true
        }

      if (returnValue == null)
        if (!reachedEnd) {
          reachedEnd = true
          JsEnd
        }
        else
          throw new NoSuchElementException("json.parse.next")
      else
        returnValue
    }

    def hasNext: Boolean = !reachedEnd

    private[this] var reachedEnd = false
    private[this] var token: JsonToken = JsonToken.NOT_AVAILABLE
    private[this] var path = Path.empty[Any]
    private[this] val trace = new mutable.Stack[JsonToken]
  }

  private[this] def jsonFactory =
    new JsonFactory()
}
