package mh

import akka.actor._
import spray.can.server.SprayCanHttpServerApp

import mh.model._
import mh.router._
import mh.simulator._


object Main extends App with SprayCanHttpServerApp {
  val router = system.actorOf(Props[RouterService], "router")
  val model = system.actorOf(Props[ModelActor], "model")
  val projector = system.actorOf(Props[Projector], "projector")
  val simulator = system.actorOf(Props[Simulator], "simulator")
  newHttpServer(router) ! Bind(interface = "localhost", port = 8000)
}
