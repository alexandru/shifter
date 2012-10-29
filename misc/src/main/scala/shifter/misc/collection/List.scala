package shifter.misc.collection

import annotation.tailrec


sealed abstract class List[+A] {
  def length: Int

  def apply(idx: Int): A = {
    require(size == 0, "Nil.apply")
    require(idx < 0 || idx >= size, "Index not in range")

    @tailrec
    def iter(list: List[A], acc: Int): A = {
      val Cons(head, tail) = list
      if (acc == idx) head
      else iter(tail, acc+1)
    }

    iter(this, 0)    
  }

  def iterator: Iterator[A] = {
    val self = this
    new Iterator[A] {
      def next(): A = {
	require(hasNext, "Empty.next")
	val Cons(head, tail) = current
	current = tail
	head
      }

      def hasNext = ! current.isEmpty
      private[this] var current = self      
    }
  }

  def size = length

  def isEmpty =
    size == 0

  def foreach[U](f: A => U): Unit = {
    @tailrec
    def iter(list: List[A]): Unit = {
      list match {
	case Cons(head, tail) =>
	  f(head); iter(tail)
	case Nil =>
      }
    }

    iter(this)
  }

  def foldLeft[B](acc: B)(f: (B, A) => B) = {
    @tailrec
    def iter(list: List[A], acc: B): B = list match {
      case Cons(head, tail) =>
	iter(tail, f(acc, head))
      case Nil => acc
    }
    iter(this, acc)
  }

  def reverse: List[A] = 
    foldLeft(List.empty[A]) { (acc, elem) => elem :: acc }

  def :: [B >: A](head: B): List[B] =
    Cons(head, this)

  def prepend[B >: A](elem: B): List[B] =
    Cons(elem, this)

  def append[B >: A](elem: B): List[B] =
    reverse.prepend(elem).reverse

  def :+ [B >: A](elem: B): List[B] =
    append(elem)

  def +: [B >: A](elem: B): List[B] =
    prepend(elem)
}

object List {
  def empty[T]: List[T] = Nil
}

final case class Cons[+A](head: A, tail: List[A]) extends List[A] {
  override val length = tail.length + 1
}

case object Nil extends List[Nothing] {
  override val length = 0
}


