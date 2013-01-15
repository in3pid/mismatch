package mh.mismatch
import concurrent._
import concurrent.duration._
import language.postfixOps

object Implicit {
  implicit val timeout = akka.util.Timeout(30 seconds)
}
