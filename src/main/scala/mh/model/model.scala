package mh.model

import akka.actor._
import org.json4s._
import spray.routing.{RequestContext}

import mh.{ Main }
import mh.JsonExtension._

// do something with the database
trait ModelMessage {
  def commit: Unit // parent scope sets transaction
}

case class User(id: Option[Int]=None, cat: List[String], skill: List[String])

import slick.driver.MySQLDriver.simple._
import Database.threadLocalSession

object Users extends Table[(Option[Int])]("users") {
  import Backer._
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def * = id.?
  def autoInc = id.? returning id

  def exists(user: User) = user.id match {
    case None => false
    case Some(id) => db withSession {
      0 < Query(Users).where(_.id is id).list.size
    }
  }
}

object Cats extends Table[(Int, String)]("cats") {
  import Backer._
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def tag = column[String]("tag")
  def * = id ~ tag
  def fields = tag

  def exists(tag: String): Boolean = db withTransaction {
    0 < Query(Cats).where(_.tag===tag).list.size
  }
  def idFor(tag: String): Int = {
    val q = Query(Cats).where(_.tag===tag).map(_.id)
    q.first
  }
}

object Skills extends Table[(Int, String)]("skills") {
  import Backer._

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def tag = column[String]("tag")
  def * = id ~ tag
  def fields = tag

  def exists(tag: String): Boolean = db withTransaction {
    0 < Query(Skills).where(_.tag===tag).list.size
  }
  def idFor(tag: String): Int = {
    val q = Query(Skills).where(_.tag===tag).map(_.id)
    q.first
  }
}

object SkillMap extends Table[(Int, Int, Int)]("skillmap") {
  import Backer._

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Int]("user_id")
  def skillId = column[Int]("skill_id")
  def * = id ~ userId ~ skillId
  def fields = userId ~ skillId

  def exists(userId: Int, skillId: Int): Boolean = db withTransaction {
    0 < Query(SkillMap).where(row=>row.userId===userId&&row.skillId===skillId).list.size
  }
}

object CatMap extends Table[(Int, Int, Int)]("catmap") {
  import Backer._
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Int]("user_id")
  def catId = column[Int]("cat_id")
  def * = id ~ userId ~ catId
  def fields = userId ~ catId

  def exists(userId: Int, catId: Int): Boolean = db withTransaction {
    0 < Query(CatMap).where(row=>row.userId===userId&&row.catId===catId).list.size
  }
}


case class UpdateUser(user: User) extends ModelMessage {
  def commit {
    val uid = 
      if (!Users.exists(user)) Users.autoInc.insert(user.id)
      else user.id.get

    user.cat foreach { s =>
      if (!Cats.exists(s)) Cats.fields.insert(s)
    }

    user.skill foreach { s =>
      if (!Skills.exists(s)) Skills.fields.insert(s)
    }

    val catIds = user.cat map Cats.idFor
    catIds foreach { catId =>
      if (!CatMap.exists(uid, catId))
        CatMap.fields.insert(uid, catId)
    }

    val skillIds = user.skill map Skills.idFor
    skillIds foreach { skillId =>
      if (!SkillMap.exists(uid, skillId))
        SkillMap.fields.insert(uid, skillId)
    }
  }
}


case class ResetModel() extends ModelMessage {
  import Backer._
  def commit {
    val ddl =  Users.ddl ++ Cats.ddl ++
      Skills.ddl ++ CatMap.ddl ++ SkillMap.ddl
    ddl.drop
    ddl.create
  }
}


object Backer {
  val db: Database = Database.forURL("jdbc:mysql://localhost/model?user=db", driver = "com.mysql.jdbc.Driver")
  def skillsForUser(userId: Int) =
    for (us <- SkillMap;
         s <- Skills if s.id == us.skillId)
      yield s.tag
}

class ModelActor extends Actor with ActorLogging {
  import Backer._
  def receive = {
    case msg: ModelMessage => db withTransaction { msg.commit }
  }
}

import mh.collection._

case class CategoryCoefficients() {
  import Backer._
  type ResultMap = Map[Int,MultiSet[Int]]
  def calculate: ResultMap = {
    var map = Map.empty[Int,MultiSet[Int]]
    val list = db withTransaction {
        val query = 
          for { skillMap <- SkillMap
                catMap <- CatMap
                if catMap.userId === skillMap.userId }
          yield (catMap.catId, skillMap.skillId)
        query.list
      }
    list foreach { elt =>
      elt match {
        case (catId, skillId) =>
          val t = map.getOrElse(catId, new MultiSet[Int])
          val m = t + skillId
          map += (catId -> m)
      }
    }
    map
  }
}
