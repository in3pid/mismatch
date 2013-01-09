package mh.model
import akka.actor._
import akka.event._

import slick.driver.MySQLDriver.simple._
import Database.threadLocalSession

import mh.model._
import spray.routing._

trait ProjectorMessage {
  def commit: Unit
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
    Backer.db.withSession { query.list.toString }
}

case class ProjectRanks(rc: RequestContext) extends ProjectorMessage {
  def commit {
    rc.complete("foo")
  }
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

