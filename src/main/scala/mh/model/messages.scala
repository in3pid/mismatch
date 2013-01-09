package mh.model.messages

trait Message

import org.json4s._
import spray.routing.{RequestContext}

case class ServiceMessage[A](
  a: A,
  requestContext: RequestContext)

case class Query(query: JObject) extends Message

case class Subscribe(
  kind: String) extends Message

case class Unsubscribe(
  kind: String) extends Message

case class Pulse() extends Message

case class Suggestions[+A](
  suggestions: List[(A, Float)]) extends Message
