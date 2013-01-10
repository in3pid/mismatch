package mh.model
import akka.actor._
import akka.event._

import slick.driver.MySQLDriver.simple._
import Database.threadLocalSession

import mh.collection._
import spray.routing._

import scala.concurrent._
import spray.caching.{LruCache, Cache}
import spray.util._

import mh.Main


trait ProjectorMessage {
  def commit: Unit
}

class Projector extends Actor with ActorLogging {

  type CacheValue = String
  val cache: Cache[CacheValue] = LruCache()

  def cachedOp[T](key: T, f: => CacheValue): Future[CacheValue] =
      cache(key) { f }

  def receive = {
    case msg: ProjectorMessage =>
      log.info(msg.toString)
      msg.commit
    case x@ _ => log.warning("unhandled message: " + x)
  }
}

case class CatSkills(map: Map[Int, MultiSet[Int]])

case class CatSkillsTransaction(
  promise: Promise[CatSkills]) extends Transaction {

  type T = CatSkills
  val query =
    for { skillMap <- SkillMap
          catMap <- CatMap
          if catMap.userId === skillMap.userId }
    yield (catMap.catId, skillMap.skillId)

  def commit {
    var map: Map[Int, MultiSet[Int]] = Map.empty
    query.list foreach { elt =>
      elt match {
        case (catId, skillId) =>
          val t = map.getOrElse(catId, new MultiSet[Int])
          val m = t + skillId
          map += (catId -> m)
      }
    }
    promise success CatSkills(map)
  }
}


import scala.concurrent.ExecutionContext.Implicits.global

case class Rank[A](tag: A, weight: Double)

case class ProjectRanks(catId: Int, n:Int=10) extends ProjectorMessage {
  val promise = Promise[List[Rank[Int]]]
  val response = promise.future
  def commit {
    val p = Promise[CatSkills]
    Main.model ! CatSkillsTransaction(p)
    p.future onSuccess {
      case CatSkills(map) =>
        val result = map mapValues { value =>
          (map.keys map { key =>
             val a = (value & map(key)).cardinality.toDouble
             val b = (value | map(key)).cardinality.toDouble
             Rank(key, a/b)
           }).toList.sortWith((a, b) => a.weight > b.weight).take(n)
        }
        promise success result(catId)
    }
  }
}

case class CatSkillsNamed(map: Map[String, MultiSet[String]])

case class CatSkillsNamedTr(
  promise: Promise[CatSkillsNamed]) extends Transaction {
  type T = CatSkillsNamed
  val query =
    for { skillMap <- SkillMap
          catMap <- CatMap if catMap.userId is skillMap.userId
          cat <- Cats if cat.id is catMap.catId
          skill <- Skills if skill.id is skillMap.skillId }
    yield (cat.tag, skill.tag)

  def commit {
    var map: Map[String, MultiSet[String]] = Map.empty
    query.list foreach { elt =>
      elt match {
        case (cat, skill) =>
          val t = map.getOrElse(cat, new MultiSet[String])
          val m = t + skill
          map += (cat -> m)
      }
    }
    promise success CatSkillsNamed(map)
  }
}


case class GetRanks(cat: String, n:Int=10) extends ProjectorMessage {
  val promise = Promise[List[Rank[String]]]
  val response = promise.future
  def commit {
    val p = Promise[CatSkillsNamed]
    Main.model ! CatSkillsNamedTr(p)
    p.future onSuccess {
      case CatSkillsNamed(map) =>
        println(map.toString)
        val result = map mapValues { value =>
          (map.keys map { key =>
             val a = (value & map(key)).cardinality.toDouble
             val b = (value | map(key)).cardinality.toDouble
             Rank(key, a/b)
           }).toList.sortWith((a, b) => a.weight > b.weight).take(n)
        }
        if (result contains cat)
          promise success result(cat)
        else
          promise failure new RuntimeException("Category not found.")
    }
  }
}
