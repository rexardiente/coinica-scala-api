name := """eos-game-store-api"""
organization := "com.example"
version := "1.0-SNAPSHOT"
scalaVersion := "2.13.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
	guice,
	ws,
	"com.typesafe.slick" %% "slick" % "3.3.2",
	"com.typesafe.play" %% "play-slick" % "5.0.0",
	"com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
	"com.github.tminglei" %% "slick-pg" % "0.19.0",
	"io.jafka" % "jeos" % "0.9.16",
	"org.typelevel" %% "cats-core" % "2.1.1",
	"com.ejisan" %% "scalauthx" % "1.0-SNAPSHOT",
  	"com.ejisan" %% "kuro-otp" % "0.0.1-SNAPSHOTS",
	"org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
)

// Adds additional packages into conf/routes
play.sbt.routes.RoutesKeys.routesImport ++= Seq(
	"utils.Binders._",
	"java.util.UUID"
)

resolvers += "sonatype snapshots".at("https://oss.sonatype.org/content/repositories/snapshots/")
resolvers += "Ejisan Github" at "https://ejisan.github.io/repo/"
Global / onChangedBuildSource := IgnoreSourceChanges
