package shifter.db

import language.existentials
import scala.util.{Failure, Success, Try}
import scala.runtime._
import scala.reflect.runtime.universe._
import java.sql.PreparedStatement

trait DBCodec[@specialized(Int, Long, Float, Double, Boolean, Short, Byte, Char) T] {
  def fromDB(value: Any): Try[T]

  def setValueInStatement(stm: PreparedStatement, idx: Int, value: T): Unit
  def setNullInStatement(stm: PreparedStatement, idx: Int): Unit
}

trait DBCodecDefaultImplicits {
  private[this] class OptionalDBCodecClass[T : DBCodec : TypeTag] extends DBCodec[Option[T]] {
    def fromDB(value: Any): Try[Option[T]] =
      if (value == null)
        Success(None)
      else if (value == None)
        Success(None)
      else value match {
        case v: Some[_] => implicitly[DBCodec[T]].fromDB(v.get).map(x => Some(x))
        case _ => implicitly[DBCodec[T]].fromDB(value).map(x => Some(x))
      }

    def setValueInStatement(stm: PreparedStatement, idx: Int, value: Option[T]) {
      value match {
        case None => implicitly[DBCodec[T]].setNullInStatement(stm, idx)
        case Some(x) => implicitly[DBCodec[T]].setValueInStatement(stm, idx, x)
      }
    }

    def setNullInStatement(stm: PreparedStatement, idx: Int) {
      implicitly[DBCodec[T]].setNullInStatement(stm, idx)
    }
  }

  implicit def OptionalDBCodec[T : DBCodec : TypeTag]: DBCodec[Option[T]] =
    new OptionalDBCodecClass[T]

  implicit object IntDBCodec extends DBCodec[Int] {
    def fromDB(value: Any) = value match {
      case x: Int => Success(x)
      case x: String => Try(x.toInt)
      case null => Try(throw null)
      case x: Any =>
        DBCodec.numberProxyFor(x).map(_.toInt) match {
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

  implicit object LongDBCodec extends DBCodec[Long] {
    def fromDB(value: Any) = value match {
      case x: Long => Success(x)
      case x: String => Try(x.toLong)
      case null => Try(throw null)
      case x: Any =>
        DBCodec.numberProxyFor(x).map(_.toLong) match {
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

  implicit object ShortDBCodec extends DBCodec[Short] {
    def fromDB(value: Any) = value match {
      case x: Short => Success(x)
      case x: String => Try(x.toShort)
      case null => Try(throw null)
      case x: Any =>
        DBCodec.numberProxyFor(x).map(_.toShort) match {
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

  implicit object ByteDBCodec extends DBCodec[Byte] {
    def fromDB(value: Any) = value match {
      case x: Byte => Success(x)
      case x: String => Try(x.toByte)
      case null => Try(throw null)
      case x: Any =>
        DBCodec.numberProxyFor(x).map(_.toByte) match {
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

  implicit object FloatDBCodec extends DBCodec[Float] {
    def fromDB(value: Any) = value match {
      case x: Float => Success(x)
      case x: String => Try(x.toFloat)
      case null => Try(throw null)
      case x: Any =>
        DBCodec.numberProxyFor(x).map(_.toFloat) match {
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

  implicit object DoubleDBCodec extends DBCodec[Double] {
    def fromDB(value: Any) = value match {
      case x: Double => Success(x)
      case x: String => Try(x.toDouble)
      case null => Try(throw null)
      case x: Any =>
        DBCodec.numberProxyFor(x).map(_.toDouble) match {
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

  implicit object BooleanDBCodec extends DBCodec[Boolean] {
    def fromDB(value: Any) = value match {
      case x: Boolean => Success(x)
      case x: String => Try(x.toBoolean)
      case null => Try(throw null)
      case x: Any =>
        DBCodec.numberProxyFor(x).map(_.toInt) match {
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
  
  implicit object StringDBCodec extends DBCodec[String] {
    def fromDB(value: Any) = value match {
      case null => Try(throw null)
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
  
  implicit object BigDecimalCodec extends DBCodec[BigDecimal] {
    def fromDB(value: Any): Try[BigDecimal] = value match {
      case null => Try(throw null)
      case x: java.math.BigDecimal => Success(BigDecimal(x))
      case x: String => Try(BigDecimal(x))
      case x: Any =>
        DBCodec.numberProxyFor(x).map(_.toDouble) match {
          case Some(nr) => Success(BigDecimal(nr))
          case None =>
            Failure(new NumberFormatException(s"For input '${x.toString}'"))
        }
    }
      
    def setValueInStatement(stm: PreparedStatement, idx: Int, value: BigDecimal) {
      stm.setBigDecimal(idx, new java.math.BigDecimal(value.toString()))
    }

    def setNullInStatement(stm: PreparedStatement, idx: Int) {
      stm.setNull(idx, java.sql.Types.DECIMAL)
    }
  }
}

object DBCodec extends DBCodecDefaultImplicits {
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