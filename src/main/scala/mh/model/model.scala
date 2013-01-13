package mh.model

import concurrent._
import akka.actor._
import org.json4s._
import spray.routing.{RequestContext}

import mh.{ Main }
import mh.JsonExtension._
import mh.Implicit._

// do something with the database
trait ModelMessage {
  def commit: Unit // parent scope sets transaction
}

trait NewModelMessage {
  type T
  def commit: T
}

trait NewTransaction {
  type T
  def commit: T
}

/**
  * Encapsulate a database transaction.
  */
trait Transaction {
  // reply type
  type T
  // will be executed in a transaction by the model
  def commit: Unit
  // a promise to reply
  def promise: Promise[T]
  // hook a transformation on the response
  def bind[A](f: PartialFunction[T,A])
          (implicit executor: ExecutionContext): Transaction = {
    promise.future.onSuccess(f)
    this
  }
}

case class User(id: Option[Int]=None, cat: List[String], skill: List[String])


import slick.driver.MySQLDriver.simple._
import Database.threadLocalSession


/* Schema */


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
    case msg: Transaction => db withTransaction { msg.commit }
    case msg: NewTransaction =>
      val response = db withTransaction { msg.commit }
      sender ! response
  }
}

/* Messages */

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
