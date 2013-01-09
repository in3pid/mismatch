package mh.simulator
import akka.actor._
import util.Random._
import mh.router.Main
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
  val high = 3
  val distribution = GeometricDistribution(0.3, 10)
  val transformer = LinearTransformer(distribution)
  def makeUser: User = {
    val r = Ssyk.randomCategory
    val x = r.toLowerCase
    val skillsForCat = (0 to high).map(_ => transformer(x)).toList
    User(None, cat=List(r), skill=skillsForCat)
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
