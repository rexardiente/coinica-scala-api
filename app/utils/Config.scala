package utils

import java.util.UUID
import scala.jdk.CollectionConverters._
import com.typesafe.config.{ ConfigFactory, ConfigList, ConfigValue }
import play.api.{ ConfigLoader, Configuration }

object Config {
	val config = ConfigFactory.load()
	val DEFAULT_SYSTEM_SCHEDULER_TIMER: Int = config.getInt("platform.default.system.scheduler")
	val GQ_DEFAULT_BATTLE_TIMER: Int = config.getInt("platform.games.GQ.battle.timer")
	val SUPPORTED_CURRENCIES: List[String] = config.getStringList("platform.supported.currencies").asScala.toList
	val SUPPORTED_SYMBOLS: List[String] = config.getStringList("platform.supported.symbols").asScala.toList
	// Ghost Quest
	val GQ: List[String] = config.getStringList("platform.games.contracts.ghostquest").asScala.toList
	val GQ_CODE: String = GQ(0)
	val GQ_TABLE: String = GQ(1)
	val GQ_SCOPE: String = GQ(2)
	val GQ_GAME_ID: UUID = UUID.fromString(GQ(3))
	val GQ_GAME_CODE: String = GQ(4)
	// Treasure Hunt
	val TH: List[String] = config.getStringList("platform.games.contracts.treasurehunt").asScala.toList
	val TH_CODE: String = TH(0)
	val TH_TABLE: String = TH(1)
	val TH_SCOPE: String = TH(2)
	val TH_GAME_ID: UUID = UUID.fromString(TH(3))
	val TH_GAME_CODE: String = TH(4)
	// Mahjong Hilo
	val MJHilo: List[String] = config.getStringList("platform.games.contracts.treasurehunt").asScala.toList
	val MJHilo_CODE: String = MJHilo(0)
	val MJHilo_TABLE: String = MJHilo(1)
	val MJHilo_SCOPE: String = MJHilo(2)
	val MJHilo_GAME_ID: UUID = UUID.fromString(MJHilo(3))
	val MJHilo_GAME_CODE: String = MJHilo(4)

	val NODE_SERVER_URI: String = config.getString("eosio.eosjs.node.server.uri")
	val EOS_TO_USD_CONVERSION: Double = config.getDouble("platform.EOS_TO_USD_CONVERSION")

	// Email Configs
	val MAILER_ADDRESS: String = config.getString("play.mailer.user")
	// Instant + (limit * (60:1 minute))
	def MAIL_EXPIRATION: Long = (java.time.Instant.now.getEpochSecond + (config.getInt("play.mailer.expiration") * 60))
	val MAIL_RANDOM_CODE_LIMIT: Int = config.getInt("play.mailer.random.code.limit")

	// Server Host URL
	private val serverAllowedURLs: List[String] = config.getStringList("play.filters.hosts.host").asScala.toList
	private val serverAllPorts: List[Int] = config.getIntList("play.filters.hosts.port").asScala.map(_.toInt).toList
	private val serverAllowedProtocols: List[String] = config.getStringList("play.filters.hosts.protocol").asScala.toList

	val MAILER_HOST: String = serverAllowedURLs(2)
	val MAILER_PORT: Int = serverAllPorts(0)
	val MAILER_PROTOCOL: String = serverAllowedProtocols(0)

	val EGS_WEB_HOST = serverAllowedURLs(3)
	val EGS_WEB_PORT: Int = serverAllPorts(2)
	val EGS_WEB_PROTOCOL: String = serverAllowedProtocols(0)

}