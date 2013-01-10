package mh.model.messages

trait Message

import org.json4s._
import spray.routing.{RequestContext}

case class Suggestions[+A](
  suggestions: List[(A, Double)]) extends Message
