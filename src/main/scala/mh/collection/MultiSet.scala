package mh.collection
import collection._

case class MultiSet[A](map: Map[A, Int]) extends Set[A] {
  override def empty = new MultiSet(Map.empty[A, Int])
  def this() = this(Map.empty[A, Int])

  def contains(key: A): Boolean = map contains key
  def weight(key: A) = map.getOrElse(key, 0)
  def cardinality = map.values.sum
  def iterator: Iterator[A] = map.keys.iterator
  def +(key: A): MultiSet[A] = {
    val w = if (contains(key)) weight(key) + 1 else 1
    MultiSet(map + (key -> w))
  }
  def -(key: A): MultiSet[A] = {
    if (contains(key)) {
      val w = weight(key)
      val m = if (w == 1) map - key else map + (key -> (w - 1))
      MultiSet(m)
    }
    else this
  }
  def |(that: MultiSet[A]): MultiSet[A] = {
    val keys = this.map.keySet | that.map.keySet
    val m = keys.foldLeft(Map.empty[A, Int]) {
      (r, key) =>
        if (contains(key)) {
          val w = if (that contains key) math.max(weight(key), that weight key)
                  else weight(key)
          r + (key -> w)
        }
        else r + (key -> that.weight(key))
    }
    MultiSet(m)
  }
  def &(that: MultiSet[A]): MultiSet[A] = {
    val keys = this.map.keySet & that.map.keySet
    val m = keys.foldLeft(Map.empty[A, Int]) {
      (r, key) => r + (key -> math.min(weight(key), that.weight(key)))
    }
    MultiSet(m)
  }

  override def equals(that: Any) : Boolean = that match {
    case rhs@ MultiSet(thatMap) =>
      (map.keySet == thatMap.keySet) &&
      map.keys.forall { key => weight(key) == rhs.weight(key) }
  }

  override def subsetOf(that: GenSet[A]): Boolean = that match {
    case rhs@ MultiSet(thatMap) =>
      (map.keySet subsetOf thatMap.keySet) &&
      map.keys.forall { key => weight(key) <= rhs.weight(key) }
  }

  def overlap(that: MultiSet[A]): Double = {
    val a = (this & that).cardinality.toDouble
    val b = (this | that).cardinality.toDouble
    a / b
  }
}
