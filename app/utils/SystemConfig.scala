package utils

import java.time.Instant
import java.util.UUID
import scala.jdk.CollectionConverters._
import com.typesafe.config.{ ConfigFactory, ConfigList, ConfigValue }
import play.api.{ ConfigLoader, Configuration }

object SystemConfig {
	val config = ConfigFactory.load()
	// val DEFAULT_SYSTEM_SCHEDULER_TIMER: Int = config.getInt("platform.default.system.scheduler")
	// val DEFAULT_WEI_VALUE: BigDecimal = BigDecimal(config.getString("platform.wei.value"))
	val SUPPORTED_CURRENCIES: List[String] = config.getStringList("platform.supported.currencies").asScala.toList
	val SUPPORTED_SYMBOLS: List[String] = config.getStringList("platform.supported.symbols").asScala.toList
	// Server Host URL
	private val serverAllowedURLs: List[String] = config.getStringList("play.filters.hosts.url").asScala.toList
	private val serverAllowedProtocols: List[String] = config.getStringList("play.filters.hosts.protocol").asScala.toList

	val PROTOCOL: String = serverAllowedProtocols(1)
	val NODE_SERVER_URI: String = s"${PROTOCOL}://${serverAllowedURLs(1)}"
	val SCALA_SERVER_URI: String = s"${PROTOCOL}://${serverAllowedURLs(0)}"
	val COINICA_WEB_HOST: String = s"${PROTOCOL}://${serverAllowedURLs(2)}"
	val MAILER_HOST: String = serverAllowedURLs(0)
	// Email Configs
	val MAILER_ADDRESS: String = config.getString("play.mailer.user")
	// Instant + (limit * (60:1 minute))
	val MAIL_RANDOM_CODE_LIMIT: Int = config.getInt("play.mailer.random.code.limit")
	val DEFAULT_MAIL_EXPIRATION: Int = config.getInt("play.mailer.expiration")
	val DEFAULT_TOKEN_EXPIRATION: Int = config.getInt("platform.token.expiration")
	def MAIL_EXPIRATION: Long = Instant.now.getEpochSecond + (DEFAULT_MAIL_EXPIRATION * 60)
	def TOKEN_EXPIRATION: Long = Instant.now.getEpochSecond + (DEFAULT_TOKEN_EXPIRATION * 60)

	// new config lists
	val DEFAULT_SYSTEM_SCHEDULER_TIMER: Int = 24
	val DEFAULT_WEI_VALUE: BigDecimal = BigDecimal("0.000000000000000001")
}