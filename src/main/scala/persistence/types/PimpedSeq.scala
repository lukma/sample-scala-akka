package persistence.types

import scala.language.implicitConversions

/**
  * Created by Gayo on 12/28/2016.
  */
class PimpedSeq[A](s: Seq[A]) {
  /**
    * Group elements of the sequence that have consecutive keys that are equal.
    */
  def groupConsecutiveKeys[K](f: (A) => K): Seq[(K, List[A])] = {
    this.s.foldRight(List[(K, List[A])]())((item: A, res: List[(K, List[A])]) =>
      res match {
        case Nil => List((f(item), List(item)))
        case (k, kLst) :: tail if k == f(item) => (k, item :: kLst) :: tail
        case _ => (f(item), List(item)) :: res
      })
  }
}

object PimpedSeq {
  implicit def seq2PimpedSeq[A](s: Seq[A]): PimpedSeq[A] = new PimpedSeq(s)
}
