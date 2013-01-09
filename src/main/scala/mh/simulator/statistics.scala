package mh.simulator

import scala.math._
import scala.util.Random._

trait RandomDistribution[A] extends Iterator[A] {
  def hasNext = true
}

case class GeometricDistribution(
  rho: Double,
  deltaMax: Int) extends RandomDistribution[Int]
{
  def signum = if (nextBoolean) 1 else -1 // sign
  def delta(δ: Int=0): Int =  // P(δ = i) = rho*(1-rho)^(i-1)
    if (δ <= deltaMax && nextDouble < rho) delta(δ + 1)
    else δ
  def next = signum * delta()
}

case class LinearTransformer(distribution: RandomDistribution[Int])
{
  def apply(x: Char): Char = {
    val δ = distribution.next
    val (α, β) = ((x + δ).toChar, (x - δ).toChar)
    if (α.isLetter) α else β
  }
  def apply(x: String): String = {
    x.map(ξ => apply(ξ))
  }
}

//trait Metric[A] extends (A, A) => Double
trait Fitness[A] extends ((A, A) => Double) // in [0, 1]

// Arithmetic Mean.

case class MeanFitness(δmax: Double)
    extends Fitness[String]
{
  def apply(lhs: Char, rhs: Char): Double = abs(lhs - rhs) / (2 * δmax)
  def apply(lhs: String, rhs: String): Double =  {
    val Δ = (lhs zip rhs) map { case (α: Char,  β: Char) => apply(α, β) }
    Δ.sum / Δ.size
  }
}


object DistanceMethod {
  val rho = 0.3
  val deltaMax = 10
  val distribution = GeometricDistribution(rho, deltaMax)
  val transformer = LinearTransformer(distribution)
  val fitness = MeanFitness(deltaMax)
}

object Ssyk {
  val alphabet = 'A' to 'J'
  val strings = for { α <- alphabet; β <- alphabet; γ <- alphabet } yield "" + α + β + γ
  def randomCategory: String = {
    def r = alphabet(nextInt(alphabet.size))
    (1 to 3).map(_ => r).addString(new StringBuilder).toString
  }
}
