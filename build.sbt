name := """eos-game-store-api"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
	guice,
	"com.typesafe.slick" %% "slick" % "3.3.2",
	"com.typesafe.play" %% "play-slick" % "5.0.0",
	"com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
	"com.github.tminglei" %% "slick-pg" % "0.19.0",
	"org.postgresql" % "postgresql" % "42.2.12",
	"com.typesafe.play" %% "play-json" % "2.8.1",
	"org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
