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

trait Transaction {
  type T
  def promise: Promise[T]
  def apply: T
}


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
      log.info("done")
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

  def apply = {
    var map: Map[Int, MultiSet[Int]] = Map.empty
    query.list foreach { elt =>
      elt match {
        case (catId, skillId) =>
          val t = map.getOrElse(catId, new MultiSet[Int])
          val m = t + skillId
          map += (catId -> m)
      }
    }
    CatSkills(map)
  }
}


import scala.concurrent.ExecutionContext.Implicits.global

case class ProjectRanks(catId: Int, n:Int=10) extends ProjectorMessage {
  val promise = Promise[List[(Int,Double)]]
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
             (key, a/b)
          }).toList.sortWith((a, b) => a._2 > b._2).take(n)
        }
        promise success result(catId)
    }
  }
}
