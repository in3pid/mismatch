package mh.collection

import org.scalatest.FunSpec

class MultiSetSuite extends FunSpec {
  describe("A MultiSet") {
    it("should count insertions (+) and removals (-)") {
      var m = new MultiSet[Int]
      assert(m.weight(1) === 0)
      m += 1
      assert(m.weight(1) === 1)
      m += 1
      assert(m.weight(1) === 2)
      m += 2
      m -= 1
      assert(m.weight(2) === 1)
      assert(m.weight(1) === 1)
      m -= 1
      assert(m.weight(1) === 0)
      assert(m.cardinality === 1)
    }
    it("should resolve equality") {
      val a = new MultiSet[Int] + 1 + 3 + 2 + 1
      val b = new MultiSet[Int] + 3 + 2 + 1 + 1
      assert(a === b)
      val c = new MultiSet[Int] + 1 + 3 + 2 + 1
      val d = new MultiSet[Int] + 3 + 2 + 2 + 1
      assert(c != d)
    }
    it("should resolve subsetOf") {
      val a = new MultiSet[Int]
      val b = new MultiSet[Int]
      assert(a subsetOf b)
      assert(b subsetOf a)
      val c = new MultiSet[Int] + 1
      assert(a subsetOf c)
      assert( !(c subsetOf a) )
    }
    it("should produce unions") {
      var a = new MultiSet[Int] + 1
      var b = new MultiSet[Int] + 2 + 2
      val c = a | b
      assert(c.weight(1) === 1)
      assert(c.weight(2) === 2)
      assert(c.cardinality === 3)
    }
    it("should produce intersections") {
      val c = (new MultiSet[Int] + 1 + 2 + 3) &
              (new MultiSet[Int] + 2 + 4 + 6)
      assert(c.weight(2) === 1)
      assert(c.cardinality === 1)
    }
    it("should have an empty intersection of disjoint multisets") {
      assert(
        ((new MultiSet[Int] + 1 + 3 + 5) &
         (new MultiSet[Int] + 2 + 4 + 6)).cardinality === 0)
    }
  }
}
