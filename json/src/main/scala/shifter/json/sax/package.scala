package shifter.json

import com.fasterxml.jackson.core.{JsonProcessingException, JsonToken, JsonParser, JsonFactory}
import scala.collection.mutable
import java.io.{Reader, InputStream, File}
import java.net.URL

package object sax {
  def parseJson(f: File): JsonIterator =
    parseJson(jsonFactory.createParser(f))

  def parseJson(content: String): JsonIterator =
    parseJson(jsonFactory.createParser(content))

  def parseJson(data: Array[Byte], off: Int, len: Int): JsonIterator =
    parseJson(jsonFactory.createParser(data, off, len))

  def parseJson(data: Array[Byte]): JsonIterator =
    parseJson(jsonFactory.createParser(data))

  def parseJson(url: URL): JsonIterator =
    parseJson(jsonFactory.createParser(url))

  def parseJson[T](in: InputStream): JsonIterator =
    parseJson(jsonFactory.createParser(in))

  def parseJson[T](r: Reader): JsonIterator =
    parseJson(jsonFactory.createParser(r))

  trait JsonIterator extends Iterator[Event] {
    def current: Event
  }
  
  def parseJson(parser: JsonParser) = new JsonIterator {
    def next(): Event = {
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

      var returnValue: Event = null

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
              returnValue = Event.String(path, parser.getValueAsString)
              popFieldName()

            case JsonToken.VALUE_FALSE =>
              ifArrayIncrementIndex()
              returnValue = Event.Bool(path, false)
              popFieldName()

            case JsonToken.VALUE_TRUE =>
              ifArrayIncrementIndex()
              returnValue = Event.Bool(path, true)
              popFieldName()

            case JsonToken.VALUE_NUMBER_FLOAT =>
              ifArrayIncrementIndex()
              returnValue = Event.Double(path, parser.getValueAsDouble)
              popFieldName()

            case JsonToken.VALUE_NUMBER_INT =>
              ifArrayIncrementIndex()
              returnValue = Event.Long(path, parser.getValueAsLong)
              popFieldName()

            case JsonToken.VALUE_NULL =>
              ifArrayIncrementIndex()
              returnValue = Event.Null(path)
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
            returnValue = Event.Error(path, ex.getMessage)
            reachedEnd = true
        }

      _current = if (returnValue == null)
        if (!reachedEnd) {
          reachedEnd = true
          Event.End
        }
        else
          throw new NoSuchElementException("json.parse.next")
      else
        returnValue

      _current
    }

    def hasNext: Boolean = !reachedEnd

    def current = _current

    private[this] var _current: Event = null
    private[this] var reachedEnd = false
    private[this] var token: JsonToken = JsonToken.NOT_AVAILABLE
    private[this] var path = Path.empty[Any]
    private[this] val trace = new mutable.Stack[JsonToken]
  }

  private[this] def jsonFactory =
    new JsonFactory()
}
