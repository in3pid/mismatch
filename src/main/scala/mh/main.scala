package mh

import akka.actor._
import akka.routing._
import spray.can.server.SprayCanHttpServerApp

import mh.model._
import mh.router._
import mh.simulator._

object Main extends App with SprayCanHttpServerApp {
  val router = system.actorOf(Props[RouterService], "router")
  val modelRouter = system.actorOf(
    Props[ModelWorker]
      .withRouter(new RoundRobinRouter(2)),
    name = "model-router")
  val simulator = system.actorOf(Props[Simulator], "simulator")
  newHttpServer(router) ! Bind(interface = "localhost", port = 8000)
}
