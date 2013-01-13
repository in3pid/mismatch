package mh.model

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
