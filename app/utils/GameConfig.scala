package utils

import java.time.Instant
import java.util.UUID
import scala.jdk.CollectionConverters._
import com.typesafe.config.{ ConfigFactory, ConfigList, ConfigValue }
import play.api.{ ConfigLoader, Configuration }

object GameConfig {
	val config = ConfigFactory.load()
	// val GQ_DEFAULT_BATTLE_TIMER: Int = config.getInt("platform.games.GQ.battle.timer")
	// Ghost Quest
	val GQ: List[String] = config.getStringList("platform.games.contracts.ghostquest").asScala.toList
	val GQ_CODE: String = GQ(0)
	val GQ_GAME_ID: UUID = UUID.fromString(GQ(1))
	val GQ_GAME_CODE: String = GQ(2)
	// Treasure Hunt
	val TH: List[String] = config.getStringList("platform.games.contracts.treasurehunt").asScala.toList
	val TH_CODE: String = TH(0)
	val TH_GAME_ID: UUID = UUID.fromString(TH(1))
	val TH_GAME_CODE: String = TH(2)
	// Mahjong Hilo
	val MJHilo: List[String] = config.getStringList("platform.games.contracts.mahjonghilo").asScala.toList
	val MJHilo_CODE: String = MJHilo(0)
	val MJHilo_GAME_ID: UUID = UUID.fromString(MJHilo(1))
	val MJHilo_GAME_CODE: String = MJHilo(2)
}