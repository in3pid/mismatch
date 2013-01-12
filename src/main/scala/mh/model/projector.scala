package mh.model
import akka.actor._
import akka.event._
import akka.pattern._

import slick.driver.MySQLDriver.simple._
import Database.threadLocalSession

import mh.collection._
import spray.routing._

import scala.concurrent._
import concurrent.duration._
import spray.caching.{LruCache, Cache}
import spray.util._

import mh.Main

trait ProjectorMessage {
  def commit: Unit
}

case class Rank[A](tag: A, weight: Double)
case class CatSkillsNamed(map: Map[String, MultiSet[String]])

trait Projection {
  type T
  def commit: T
}


case class NewCatSkillsNamedTr() extends NewTransaction {
  type T = CatSkillsNamed
  val query =
    for { skillMap <- SkillMap
          catMap <- CatMap if catMap.userId is skillMap.userId
          cat <- Cats if cat.id is catMap.catId
          skill <- Skills if skill.id is skillMap.skillId }
    yield (cat.tag, skill.tag)

  def commit = {
    var map: Map[String, MultiSet[String]] = Map.empty
    query.list foreach { elt =>
      elt match {
        case (cat, skill) =>
          val t = map.getOrElse(cat, new MultiSet[String])
          val m = t + skill
          map += (cat -> m)
      }
    }
    CatSkillsNamed(map)
  }
}

case class GetRanks(cat: String, n:Int=10)

class Projector extends Actor with ActorLogging {
  implicit val timeout = akka.util.Timeout(30 seconds)
  def receive = {
    case msg: ProjectorMessage =>
      log.info(msg.toString)
      msg.commit
    case GetRanks(cat, n) =>
      ask(Main.model, NewCatSkillsNamedTr()) map {
        case CatSkillsNamed(in) =>
          in.mapValues { value =>
            (in.keys.map { key => Rank(key, value overlap in(key)) })
            .toList.sortWith((a, b) => a.weight > b.weight).take(n)
          }.getOrElse(cat, Nil)
      } pipeTo sender
    case x@ _ => log.warning("unhandled message: " + x)
  }
}
