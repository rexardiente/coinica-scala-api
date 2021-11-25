package utils

import java.time.Instant
import java.util.UUID
import scala.jdk.CollectionConverters._
import com.typesafe.config.{ ConfigFactory, ConfigList, ConfigValue }
import play.api.{ ConfigLoader, Configuration }

object SystemConfig {
	val config = ConfigFactory.load()

	val DEFAULT_HOST: String = "http://127.0.0.1:9000"
	val SUPPORTED_CURRENCIES: List[String] = config.getStringList("platform.supported.currencies").asScala.toList
	val SUPPORTED_SYMBOLS: List[String] = config.getStringList("platform.supported.symbols").asScala.toList
	// Server Host URL
	private val serverAllowedURLs: List[String] = config.getStringList("play.filters.hosts.url").asScala.toList
	private val serverAllowedProtocols: List[String] = config.getStringList("play.filters.hosts.protocol").asScala.toList

	val NODE_SERVER_URI: String = "node_api"
	val SCALA_SERVER_URI: String = "https://api.coinica.net/s1"
	val COINICA_WEB_HOST: String = "https://coinica.net"
	val MAILER_HOST: String = "https://api.coinica.net/s1"
	// Email Configs
	val MAILER_ADDRESS: String = config.getString("play.mailer.user")
	val DEFAULT_EXPIRATION: Int = 15
	val DEFAULT_SYSTEM_SCHEDULER_TIMER: Int = 24
	val DEFAULT_WEI_VALUE: BigDecimal = BigDecimal("0.000000000000000001")
}