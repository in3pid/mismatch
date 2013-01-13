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
import mh.model.Backer.{db => database}
import mh.Implicit._

trait ProjectorMessage {
  def commit: Unit
}
trait Projection {
  type T
  def commit: T
}

case class Rank[A](tag: A, weight: Double)
case class CatSkills(map: Map[String, MultiSet[String]])

case class CatSkillsTr() extends NewTransaction {
  type T = CatSkills
  val query =
    for { skillMap <- SkillMap
          catMap <- CatMap if catMap.userId is skillMap.userId
          cat <- Cats if cat.id is catMap.catId
          skill <- Skills if skill.id is skillMap.skillId }
    yield (cat.tag, skill.tag)

  def commit = database withSession {
    var map: Map[String, MultiSet[String]] = Map.empty
    query foreach { elt =>
      elt match {
        case (cat, skill) =>
          val t = map.getOrElse(cat, new MultiSet[String])
          val m = t + skill
          map += (cat -> m)
      }
    }
    CatSkills(map)
  }
}


import scala.concurrent.ExecutionContext.Implicits.global

case class GetRanks(cat: String, n:Int=10) {
  def commit: Future[List[Rank[String]]] = {
      ask(Main.model, CatSkillsTr()) map {
        case CatSkills(in) =>
          in.mapValues { value =>
            (in.keys.map { key => Rank(key, value overlap in(key)) })
            .toList.sortWith((a, b) => a.weight > b.weight).take(n)
          }.getOrElse(cat, Nil)
      }
  }
}

class Projector extends Actor with ActorLogging {
  def receive = {
    case msg: ProjectorMessage =>
      log.info(msg.toString)
      msg.commit
    // case GetRanks(cat, n) =>
    //   ask(Main.model, CatSkillsTr()) map {
    //     case CatSkills(in) =>
    //       in.mapValues { value =>
    //         (in.keys.map { key => Rank(key, value overlap in(key)) })
    //         .toList.sortWith((a, b) => a.weight > b.weight).take(n)
    //       }.getOrElse(cat, Nil)
    //   } pipeTo sender
    case x@ _ => log.warning("unhandled message: " + x)
  }
}
