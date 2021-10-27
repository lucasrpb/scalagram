name := """scalagram"""
organization := "com.scalagram"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.13"
lazy val pulsar4sVersion = "2.7.3"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

libraryDependencies += "com.typesafe.slick" %% "slick" % "3.3.3"
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.14"
libraryDependencies += "com.typesafe.play" %% "play-slick" % "5.0.0"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
libraryDependencies += "com.github.tminglei" %% "slick-pg" % "0.19.7"
libraryDependencies += "com.github.tminglei" %% "slick-pg_joda-time" % "0.19.7"
libraryDependencies += "com.github.tminglei" %% "slick-pg_jts" % "0.19.7"
libraryDependencies += "com.github.tminglei" %% "slick-pg_json4s" % "0.19.7"
libraryDependencies += "com.github.tminglei" %% "slick-pg_play-json" % "0.19.7"
libraryDependencies += "com.yugabyte" % "jedis" % "2.9.0-yb-16"

libraryDependencies ++= Seq(

  "org.apache.pulsar" % "pulsar-client" % "2.8.0",
  "org.apache.pulsar" % "pulsar-client-admin" % "2.8.0",

  "com.sksamuel.pulsar4s" %% "pulsar4s-core" % pulsar4sVersion,

  // for the akka-streams integration
  "com.sksamuel.pulsar4s" %% "pulsar4s-akka-streams" % pulsar4sVersion,

  // if you want to use play-json for schemas
  "com.sksamuel.pulsar4s" %% "pulsar4s-play-json" % pulsar4sVersion,

 // "io.swagger" %% "swagger-play2" % "1.7.1"
)

dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.1"
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.scalagram.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.scalagram.binders._"
