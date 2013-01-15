package mh.model

import concurrent._
import akka.actor._
import org.json4s._
import spray.routing.{RequestContext}

import mh.{ Main }
import mh.collection._
import mh.Implicit._


case class Rank[A](tag: A, weight: Double)
case class CatSkills(map: Map[String, MultiSet[String]])

case class SkillsForCat(tag: String, n: Int = 10)

case class ResetModel()
case class User(id: Option[Int]=None, cat: List[String], skill: List[String])
case class GetRanks(cat: String, n:Int=10)


import slick.driver.MySQLDriver.simple._
import Database.threadLocalSession

object Backer {
  val db: Database = Database.forURL(
    "jdbc:mysql://localhost/model?user=db",
    driver = "com.mysql.jdbc.Driver")
}


class ModelWorker extends Actor with ActorLogging {
  import Backer.db

  val querySkillsForCats =
    for {
      skillMap <- SkillMap
      catMap <- CatMap if catMap.userId is skillMap.userId
      cat <- Cats if cat.id is catMap.catId
      skill <- Skills if skill.id is skillMap.skillId }
    yield (cat.tag, skill.tag)

  def mapCatSkills() = db.withSession {
    var map: Map[String, MultiSet[String]] = Map.empty
    querySkillsForCats foreach { elt =>
      elt match {
        case (cat, skill) =>
          val t = map.getOrElse(cat, new MultiSet[String])
          val m = t + skill
          map += (cat -> m)
      }
    }
    map
  }

  def skillsForCat(cat: String) = mapCatSkills()(cat)

  def getRanks(cat: String, n: Int) = {
    val in = mapCatSkills()
    in.mapValues { value =>
      (in.keys.map { key => Rank(key, value overlap in(key)) })
        .toList.sortWith((a, b) => a.weight > b.weight).take(n)
    }.getOrElse(cat, Nil)
  }

  def resetModel() = db.withSession {
    val ddl = Users.ddl ++ Cats.ddl ++
      Skills.ddl ++ CatMap.ddl ++ SkillMap.ddl
    ddl.drop
    ddl.create
  }

  def updateUser(user: User) = db.withSession {
    val uid =
      if (!Users.exists(user)) Users.autoInc.insert(user.id)
      else user.id.get

    val catIds = user.cat map { s =>
      if (!Cats.exists(s)) Cats.autoIncFields.insert(s)
      else Cats.idFor(s)
    }

    val skillIds = user.skill map { s =>
      if (!Skills.exists(s)) Skills.autoIncFields.insert(s)
      else Skills.idFor(s)
    }

    catIds foreach { catId =>
      if (!CatMap.exists(uid, catId)) CatMap.fields.insert(uid, catId)
    }

    skillIds foreach { skillId =>
      if (!SkillMap.exists(uid, skillId))
        SkillMap.fields.insert(uid, skillId)
    }
  }

  def receive = {
    case SkillsForCat(cat, n) =>
      sender ! skillsForCat(cat)
    case user: User =>
      sender ! updateUser(user)
    case ResetModel() =>
      sender ! resetModel()
    case GetRanks(cat, n) =>
      sender ! getRanks(cat, n)
  }

}
