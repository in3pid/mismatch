package mh.simulator
import akka.actor._
import util.Random._
import mh.Main
import mh.model._


case class AddUsers(n:Int=500)

object Randomizer {
  val high = 5
  val distribution = GeometricDistribution(0.3, 10)
  val transformer = LinearTransformer(distribution)
  def makeUser: User = {
    val r = Ssyk.randomCategory
    val base = r.toLowerCase
    val skills = (1 to nextInt(high))
      .map(_ => transformer(base)).toList
    User(None, cat=List(r), skill=skills)
  }
}

class Simulator extends Actor with ActorLogging {
  def addUsers(n:Int=500) = {
    (1 to n) foreach { _ =>
      val user = Randomizer.makeUser
      Main.modelRouter ! user
    }
  }

  def receive = {
    case AddUsers(n) =>
      sender ! addUsers(n)
    case x@ _ => log.info(x.toString)
  }
}
