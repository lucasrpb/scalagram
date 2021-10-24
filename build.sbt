name := """scalagram"""
organization := "com.scalagram"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.6"

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

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.scalagram.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.scalagram.binders._"
