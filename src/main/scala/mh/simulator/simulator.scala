package mh.simulator
import akka.actor._
import util.Random._
import mh.Main
import mh.model._

trait SimulatorMessage {
  def commit: Unit
}

case class AddUsers(n:Int=500) extends SimulatorMessage {
  def commit {
    (1 to n) foreach { _ =>
      val u = Randomizer.makeUser
      Main.model ! UpdateUser(u)
    }
  }
}

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
  def receive = {
    case msg: SimulatorMessage =>
      log.info("Simulator: " + msg + " committing...")
      msg.commit
      log.info("Simulator done.")
    case x@ _ => log.info(x.toString)
  }
}
