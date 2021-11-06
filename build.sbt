import com.typesafe.sbt.packager.docker.DockerChmodType

name := """coinica-scala-api"""
organization := "net.coinica"
version := "1.0-SNAPSHOT"
scalaVersion := "2.13.1"

lazy val root = (project in file("."))
	.enablePlugins(PlayScala)
	.enablePlugins(DockerPlugin)

// mappings in Docker := mappings.value
packageName in Docker := packageName.value
dockerBaseImage in Docker := "openjdk:8-jre-alpine"
version in Docker := version.value
dockerExposedPorts ++= Seq(9000, 9001)
dockerAdditionalPermissions in Docker += (DockerChmodType.UserGroupPlusExecute, "/opt/docker/bin/eos-game-store-api")
dockerChmodType in Docker := DockerChmodType.UserGroupWriteExecute
javaOptions in Universal ++= Seq("-Dpidfile.path=/dev/null")
libraryDependencies ++= Seq(
	guice,
	ws,
	"joda-time" % "joda-time" % "2.10.13",
	"com.enragedginger" %% "akka-quartz-scheduler" % "1.9.1-akka-2.6.x",
	"com.typesafe.play" %% "play-mailer" % "8.0.1",
	"com.typesafe.play" %% "play-mailer-guice" % "8.0.1",
	"com.typesafe.slick" %% "slick" % "3.3.3",
	"com.typesafe.play" %% "play-slick" % "5.0.0",
	"com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
	"com.github.tminglei" %% "slick-pg" % "0.19.7",
	"com.github.tminglei" %% "slick-pg_core" % "0.19.7",
	"com.github.tminglei" %% "slick-pg_play-json" % "0.19.7",
	"io.jafka" % "jeos" % "0.9.16",
	"org.typelevel" %% "cats-core" % "2.1.1",
	"org.webjars.bower" % "compass-mixins" % "0.12.7",
	// "com.ejisan" %% "scalauthx" % "1.0-SNAPSHOT",
 //  "com.ejisan" %% "kuro-otp" % "0.0.1-SNAPSHOTS",
	"org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
)
// Adds additional packages into conf/routes
play.sbt.routes.RoutesKeys.routesImport ++= Seq(
	"utils.Binders._",
	"java.util.UUID"
)
// scalacOptions in Compile ++= Seq("-Xmax-classfile-name", "128")
resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += "sonatype snapshots".at("https://oss.sonatype.org/content/repositories/snapshots/")
// resolvers += "Ejisan Github" at "https://ejisan.github.io/repo/"
Global / onChangedBuildSource := IgnoreSourceChanges
