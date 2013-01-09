package mh.model
import akka.actor._
import akka.event._

import slick.driver.MySQLDriver.simple._
import Database.threadLocalSession

import mh.collection._
import spray.routing._

trait ProjectorMessage {
  def commit: Unit
}

class Projector extends Actor with ActorLogging {
  def receive = {
    case msg: ProjectorMessage =>
      log.info(msg.toString)
      msg.commit
      log.info("done")
    case x@ _ => log.warning("unhandled message: " + x)
  }
}


trait Projection {
  def apply(): String
}

case class TestProjection() extends Projection {
  def apply(): String = {
    "test"
  }
}

case class UserProjection(id: Int) extends Projection {
  val query = 
    for { map <- SkillMap if map.userId === id
          skill <- Skills if skill.id === map.skillId }
    yield skill.tag

  def apply(): String =
    Backer.db withSession { query.list.toString }
}


case class ProjectRanks(rc: RequestContext) extends ProjectorMessage {
  val query =
    for { skillMap <- SkillMap
          catMap <- CatMap
          if catMap.userId === skillMap.userId }
    yield (catMap.catId, skillMap.skillId)

  def commit {
    var map: Map[Int, MultiSet[Int]] = Map.empty
    val list = Backer.db withTransaction { query.list }
    list foreach { elt =>
      elt match {
        case (catId, skillId) =>
          val t = map.getOrElse(catId, new MultiSet[Int])
          val m = t + skillId
          map += (catId -> m)
      }
    }
    rc.complete(map.toString)
  }
}

