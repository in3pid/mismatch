package mh.router

import mh.model.messages._
import akka.actor._

class Router extends Actor with ActorLogging {
  def receive = {
    case (msg: Message, x: Any) =>
      sender ! (msg, x)
  }
}

import akka.actor.Props
import spray.can.server.SprayCanHttpServerApp

import mh.model.{ ModelActor, Projector, Backer }
import mh.simulator._

import slick.driver.MySQLDriver.simple._
import Database.threadLocalSession


object Main extends App with SprayCanHttpServerApp {
  val router = system.actorOf(Props[Router], "router")
  val service = system.actorOf(Props[RouterService], "http-service")
  val model = system.actorOf(Props[ModelActor], "model")
  val projector = system.actorOf(Props[Projector], "projector")
  val simulator = system.actorOf(Props[Simulator], "simulator")
  newHttpServer(service) ! Bind(interface = "localhost", port = 8000)
}
