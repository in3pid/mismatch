package mh

import org.json4s._
import org.json4s.native._

object JsonExtension {
  implicit val formats = org.json4s.DefaultFormats
  implicit def stringToJson(s: String): JValue =
    JsonParser.parse(s)
}
