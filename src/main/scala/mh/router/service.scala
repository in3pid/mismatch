package mh.router

import akka.actor._
import concurrent._
import concurrent.duration._
import spray.routing.{HttpService, RequestContext}
import spray.http._
import spray.http.MediaTypes._
import mh.model.messages._

// string or json --> bson
import mh.JsonExtension._

import org.json4s._
import org.json4s.native._

import mh.model._
import mh.simulator._

class RouterService extends Actor with ActorLogging with HttpService {
  def actorRefFactory = context
  def futureWrap(p: Projection): Future[HttpResponse] =
    future { p() } map { m => HttpResponse(entity = HttpEntity(m)) }
  val routes =
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
      path("simulator/add-users") {
        complete {
          Main.simulator ! AddUsers()
          "Sent start message to simulator."
        }
      } ~
      path("projector/project-ranks") {
        rc => Main.projector ! ProjectRanks(rc)
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

  def completeContexts: Actor.Receive = {
    case ServiceMessage(msg: Any, rc) =>
      rc.complete(msg.toString)
  }
  def receive = runRoute(routes) orElse completeContexts
}
