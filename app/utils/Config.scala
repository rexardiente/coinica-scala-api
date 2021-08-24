package utils

import java.util.UUID
import scala.jdk.CollectionConverters._
import com.typesafe.config.{ ConfigFactory, ConfigList, ConfigValue }
import play.api.{ ConfigLoader, Configuration }

object Config {
	val config = ConfigFactory.load()
	val DEFAULT_SYSTEM_SCHEDULER_TIMER: Int = config.getInt("platform.default.system.scheduler")
	val DEFAULT_WEI_VALUE: BigDecimal = BigDecimal(config.getString("platform.wei.value"))
	val GQ_DEFAULT_BATTLE_TIMER: Int = config.getInt("platform.games.GQ.battle.timer")
	val SUPPORTED_CURRENCIES: List[String] = config.getStringList("platform.supported.currencies").asScala.toList
	val SUPPORTED_SYMBOLS: List[String] = config.getStringList("platform.supported.symbols").asScala.toList
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
	// Server Host URL
	private val serverAllowedURLs: List[String] = config.getStringList("play.filters.hosts.url").asScala.toList
	private val serverAllowedProtocols: List[String] = config.getStringList("play.filters.hosts.protocol").asScala.toList

	val PROTOCOL: String = serverAllowedProtocols(0)
	val NODE_SERVER_URI: String = s"${PROTOCOL}://${serverAllowedURLs(1)}"
	val SCALA_SERVER_URI: String = s"${PROTOCOL}://${serverAllowedURLs(0)}"
	val COINICA_WEB_HOST: String = s"${PROTOCOL}://${serverAllowedURLs(2)}"
	val MAILER_HOST: String = serverAllowedURLs(0)
	// Email Configs
	val MAILER_ADDRESS: String = config.getString("play.mailer.user")
	// Instant + (limit * (60:1 minute))
	val MAIL_RANDOM_CODE_LIMIT: Int = config.getInt("play.mailer.random.code.limit")
	def MAIL_EXPIRATION: Long = (java.time.Instant.now.getEpochSecond + (config.getInt("play.mailer.expiration") * 60))
	def TOKEN_EXPIRATION: Long = (java.time.Instant.now.getEpochSecond + (config.getInt("platform.token.expiration") * 60))
}