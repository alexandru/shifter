package shifter.db

import language.existentials
import scala.util.{Failure, Success, Try}
import scala.runtime._
import scala.reflect.runtime.universe._
import java.sql.PreparedStatement

trait DBValue[@specialized(Int, Long, Float, Double, Boolean, Short, Byte, Char) T] {
  def fromDB(value: Any): Try[T]

  def setValueInStatement(stm: PreparedStatement, idx: Int, value: T): Unit
  def setNullInStatement(stm: PreparedStatement, idx: Int): Unit
}

trait DBValueDefaultImplicits {
  private[this] class OptionalDBValueClass[T : DBValue : TypeTag] extends DBValue[Option[T]] {
    def fromDB(value: Any): Try[Option[T]] =
      if (value == null)
        Success(None)
      else if (value == None)
        Success(None)
      else value match {
        case v: Some[_] => implicitly[DBValue[T]].fromDB(v.get).map(x => Some(x))
        case _ => implicitly[DBValue[T]].fromDB(value).map(x => Some(x))
      }

    def setValueInStatement(stm: PreparedStatement, idx: Int, value: Option[T]) {
      value match {
        case None => implicitly[DBValue[T]].setNullInStatement(stm, idx)
        case Some(x) => implicitly[DBValue[T]].setValueInStatement(stm, idx, x)
      }
    }

    def setNullInStatement(stm: PreparedStatement, idx: Int) {
      implicitly[DBValue[T]].setNullInStatement(stm, idx)
    }
  }

  implicit def OptionalDBValue[T : DBValue : TypeTag]: DBValue[Option[T]] =
    new OptionalDBValueClass[T]

  implicit object IntDBValue extends DBValue[Int] {
    def fromDB(value: Any) = value match {
      case x: Int => Success(x)
      case x: String => Try(x.toInt)
      case null => throw null
      case x: Any =>
        DBValue.numberProxyFor(x).map(_.toInt) match {
          case Some(nr) => Success(nr)
          case None =>
            Failure(new NumberFormatException(s"For input '${x.toString}'"))
        }
    }

    def setValueInStatement(stm: PreparedStatement, idx: Int, value: Int) {
      stm.setInt(idx, value)
    }

    def setNullInStatement(stm: PreparedStatement, idx: Int) {
      stm.setNull(idx, java.sql.Types.INTEGER)
    }
  }

  implicit object LongDBValue extends DBValue[Long] {
    def fromDB(value: Any) = value match {
      case x: Long => Success(x)
      case x: String => Try(x.toLong)
      case null => throw null
      case x: Any =>
        DBValue.numberProxyFor(x).map(_.toLong) match {
          case Some(nr) => Success(nr)
          case None =>
            Failure(new NumberFormatException(s"For input '${x.toString}'"))
        }
    }

    def setValueInStatement(stm: PreparedStatement, idx: Int, value: Long) {
      stm.setLong(idx, value)
    }

    def setNullInStatement(stm: PreparedStatement, idx: Int) {
      stm.setNull(idx, java.sql.Types.INTEGER)
    }
  }

  implicit object ShortDBValue extends DBValue[Short] {
    def fromDB(value: Any) = value match {
      case x: Short => Success(x)
      case x: String => Try(x.toShort)
      case null => throw null
      case x: Any =>
        DBValue.numberProxyFor(x).map(_.toShort) match {
          case Some(nr) => Success(nr)
          case None =>
            Failure(new NumberFormatException(s"For input '${x.toString}'"))
        }
    }

    def setValueInStatement(stm: PreparedStatement, idx: Int, value: Short) {
      stm.setShort(idx, value)
    }

    def setNullInStatement(stm: PreparedStatement, idx: Int) {
      stm.setNull(idx, java.sql.Types.INTEGER)
    }
  }

  implicit object ByteDBValue extends DBValue[Byte] {
    def fromDB(value: Any) = value match {
      case x: Byte => Success(x)
      case x: String => Try(x.toByte)
      case null => throw null
      case x: Any =>
        DBValue.numberProxyFor(x).map(_.toByte) match {
          case Some(nr) => Success(nr)
          case None =>
            Failure(new NumberFormatException(s"For input '${x.toString}'"))
        }
    }

    def setValueInStatement(stm: PreparedStatement, idx: Int, value: Byte) {
      stm.setByte(idx, value)
    }

    def setNullInStatement(stm: PreparedStatement, idx: Int) {
      stm.setNull(idx, java.sql.Types.INTEGER)
    }
  }

  implicit object FloatDBValue extends DBValue[Float] {
    def fromDB(value: Any) = value match {
      case x: Float => Success(x)
      case x: String => Try(x.toFloat)
      case null => throw null
      case x: Any =>
        DBValue.numberProxyFor(x).map(_.toFloat) match {
          case Some(nr) => Success(nr)
          case None =>
            Failure(new NumberFormatException(s"For input '${x.toString}'"))
        }
    }

    def setValueInStatement(stm: PreparedStatement, idx: Int, value: Float) {
      stm.setFloat(idx, value)
    }

    def setNullInStatement(stm: PreparedStatement, idx: Int) {
      stm.setNull(idx, java.sql.Types.FLOAT)
    }
  }

  implicit object DoubleDBValue extends DBValue[Double] {
    def fromDB(value: Any) = value match {
      case x: Double => Success(x)
      case x: String => Try(x.toDouble)
      case null => throw null
      case x: Any =>
        DBValue.numberProxyFor(x).map(_.toDouble) match {
          case Some(nr) => Success(nr)
          case None =>
            Failure(new NumberFormatException(s"For input '${x.toString}'"))
        }
    }

    def setValueInStatement(stm: PreparedStatement, idx: Int, value: Double) {
      stm.setDouble(idx, value)
    }

    def setNullInStatement(stm: PreparedStatement, idx: Int) {
      stm.setNull(idx, java.sql.Types.DOUBLE)
    }
  }

  implicit object BooleanDBValue extends DBValue[Boolean] {
    def fromDB(value: Any) = value match {
      case x: Boolean => Success(x)
      case x: String => Try(x.toBoolean)
      case null => throw null
      case x: Any =>
        DBValue.numberProxyFor(x).map(_.toInt) match {
          case Some(nr) => Success(if (nr == 0) false else true)
          case None =>
            Failure(new NumberFormatException(s"For input '${x.toString}'"))
        }
    }

    def setValueInStatement(stm: PreparedStatement, idx: Int, value: Boolean) {
      stm.setBoolean(idx, value)
    }

    def setNullInStatement(stm: PreparedStatement, idx: Int) {
      stm.setNull(idx, java.sql.Types.BOOLEAN)
    }
  }
  
  implicit object StringDBValue extends DBValue[String] {
    def fromDB(value: Any) = value match {
      case null => throw null
      case x: String => Success(x)
      case x: Any =>
        Failure(new NumberFormatException(s"For input '${x.toString}'"))
    }

    def setValueInStatement(stm: PreparedStatement, idx: Int, value: String) {
      stm.setString(idx, value)
    }

    def setNullInStatement(stm: PreparedStatement, idx: Int) {
      stm.setNull(idx, java.sql.Types.VARCHAR)
    }
  }
}

object DBValue extends DBValueDefaultImplicits {
  def numberProxyFor(value: Any): Option[ScalaNumberProxy[_]] =
    value match {
      case v : Byte => Some(new RichByte(v))
      case v : Short => Some(new RichShort(v))
      case v : Char => Some(new RichChar(v))
      case v : Long => Some(new RichLong(v))
      case v : Float => Some(new RichFloat(v))
      case v : Double => Some(new RichDouble(v))
      case v : Boolean => Some(new RichInt(if (v) 1 else 0))
      case v : Int => Some(new RichInt(v))
      case _ => None
    }
}