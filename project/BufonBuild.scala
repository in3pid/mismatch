import sbt._
import sbt.Keys._

object BufonBuild extends Build {
  import BuildSettings._
  import Dependencies._

  lazy val bufon = Project(
    id = "bufon",
    base = file("."),
    settings = buildSettings ++ List(
      name := "Bufon",
      libraryDependencies ++= List(
        akka,
        json4sNative,
        sprayCan,
        sprayRouting,
        sprayHttpx,
        sprayClient,
        sl4j,
        slick,
        h2,
        mysql)))
}

object BuildSettings {

  val buildOrganization = "mh"
  val buildVersion = "0.0.1-SNAPSHOT"
  val buildScalaVersion = "2.10.0"

  val buildSettings = Project.defaultSettings ++ Seq(
    organization := buildOrganization,
    version := buildVersion,
    scalaVersion := buildScalaVersion,
    resolvers ++= Repositories.all,
    scalacOptions ++= Seq("-deprecation", "-unchecked"))
}

object Repositories {
  val sprayRepo = "Spray Releases Repository" at "http://repo.spray.io"
  val typeSafeRepo = "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases"
  val sonaSnap = "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  val all = Seq(sprayRepo, typeSafeRepo, sonaSnap)
}

object Dependencies {
  val akka = "com.typesafe.akka" % "akka-actor_2.10" % "2.1.0"
  val salat = "com.novus" % "salat_2.10" % "1.9.2-SNAPSHOT"
  val sprayCan = "io.spray" % "spray-can" % "1.1-M7"
  val sprayRouting = "io.spray" % "spray-routing" % "1.1-M7"
  val sprayHttpx = "io.spray" % "spray-httpx" % "1.1-M7"
  val sprayClient = "io.spray" % "spray-client" % "1.1-M7"
  val json4sNative = "org.json4s" % "json4s-native_2.10" % "3.1.1-SNAPSHOT"
  val json4sMongo = "org.json4s" % "json4s-mongo_2.10" % "3.1.1-SNAPSHOT"
  val casbah = "org.mongodb" % "casbah-core_2.10" % "2.5.0-SNAPSHOT"
  val slick = "com.typesafe" % "slick_2.10" % "1.0.0-RC1"
  val sl4j = "org.slf4j" % "slf4j-nop" % "1.6.4"
  val h2 = "com.h2database" % "h2" % "1.3.166"
  val mysql = "mysql" % "mysql-connector-java" % "5.1.16"
}
