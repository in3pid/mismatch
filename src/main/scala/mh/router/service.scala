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
import mh.model.messages._

// string or json --> bson
import mh.JsonExtension._

import org.json4s._
import org.json4s.native._
import org.json4s.native.Serialization._

import mh.model._
import mh.simulator._
import mh.Main

class RouterService extends Actor with ActorLogging with HttpService {
  implicit val formats = DefaultFormats
  implicit val timeout = akka.util.Timeout(30 seconds)
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
          Main.model ! ResetModel()
          "Sent reset message to model."
        }
      } ~
      path("simulate/add-users") {
        complete {
          Main.simulator ! AddUsers()
          "Sent start message to simulator."
        }
      } ~
      path("ranks" / PathElement) { elt =>
        complete {
          (Main.projector ? GetRanks(elt))
            .mapTo[List[Rank[String]]]
            .map { write(_) }
        }
      }
    } ~
    (post | parameter('method ! "post")) {
      path("update") { rc =>
        rc.complete {
          val json: JValue = rc.request.entity.asString
          val user = json.extract[User]
          Main.model ! UpdateUser(user)
          "Sent " + user
        }
      }
    }
  def receive = runRoute(route)
}
