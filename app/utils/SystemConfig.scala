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
	// Email Configs
	val MAILER_ADDRESS: String = config.getString("play.mailer.user")

	// auto generated form the DB..
	var DEFAULT_WEI_VALUE: BigDecimal = 0
	var DEFAULT_SYSTEM_SCHEDULER_TIMER: Int = 0
	var DEFAULT_EXPIRATION: Int = 0

	var NODE_SERVER_URI: String = ""
	var SCALA_SERVER_URI: String = ""
	var COINICA_WEB_HOST: String = ""
	var MAILER_HOST: String = ""
}