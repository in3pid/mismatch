package mh.model

import concurrent.Promise
import akka.actor._
import org.json4s._
import spray.routing.{RequestContext}

import mh.{ Main }
import mh.JsonExtension._

// do something with the database
trait ModelMessage {
  def commit: Unit // parent scope sets transaction
}


/** 
  * Encapsulate a database transaction.
  *  usage: db withTransaction { Transaction.apply }
  *  model will set promise accept to function value
  */
trait Transaction {
  type T
  def promise: Promise[T]
  def commit: Unit
}

case class User(id: Option[Int]=None, cat: List[String], skill: List[String])


import slick.driver.MySQLDriver.simple._
import Database.threadLocalSession


/* Schema */


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
  def autoIncFields = tag returning id

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
  def autoIncFields = tag returning id

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
    case msg: Transaction => 
      db withTransaction { msg.commit }
  }
}


/* Messags */



case class UpdateUser(user: User) extends ModelMessage {
  def commit {
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
}


case class ResetModel() extends ModelMessage {
  def commit {
    val ddl = Users.ddl ++ Cats.ddl ++
      Skills.ddl ++ CatMap.ddl ++ SkillMap.ddl
    ddl.drop
    ddl.create
  }
}
