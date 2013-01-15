package mh.router

import akka.actor._
import akka.pattern._
import concurrent._
import concurrent.duration._
import spray.routing.{HttpService, RequestContext}
import spray.routing.directives.CachingDirectives
import CachingDirectives._
import spray.caching.LruCache
import spray.caching._
import spray.http._
import spray.http.MediaTypes._

import org.json4s._
import org.json4s.native._
import org.json4s.native.Serialization._

import mh.model._
import mh.simulator._
import mh.Main
import mh.Implicit._

import mh.collection._

class RouterService extends Actor with ActorLogging with HttpService {
  implicit val formats = DefaultFormats
  def actorRefFactory = context
  val route =
    get {
      path("stop") {
        complete {
          Main.system.scheduler.scheduleOnce(Duration(1, "sec")) {
            Main.system.shutdown()
          }
          "Shutting down..."
        }
      } ~
      path("model/reset") {
        complete {
          Main.modelRouter ! ResetModel()
          "Sent reset message to model."
        }
      } ~
      path("simulate/add-users") {
        complete {
          Main.simulator ! AddUsers()
          "Sent start message to simulator."
        }
      } ~
      path("project/ranks" / PathElement) { elt =>
        complete {
          ask(Main.modelRouter, GetRanks(elt))
          .mapTo[List[Rank[String]]]
          .map { write(_) }
        }
      } ~
      path("project/skills" / PathElement) { elt =>
        complete {
          ask(Main.modelRouter, SkillsForCat(elt))
            .mapTo[MultiSet[String]]
            .map(_.toString)
        }
      }
    } ~
    (post | parameter('method ! "post")) {
      path("update") { rc =>
        rc.complete {
          val json: JValue = JsonParser.parse(rc.request.entity.asString)
          val user = json.extract[User]
          Main.modelRouter ! user
          "Sent " + user
        }
      }
    }
  def receive = runRoute(route)
}
