package mh.collection
import collection.{ Set, SetLike }

case class MultiSet[A](map: Map[A, Int]) extends Set[A] {
  def this() = this(Map.empty[A, Int])
  def contains(key: A): Boolean = map contains key
  def weight(key: A) = map(key)
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
}
