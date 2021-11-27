package utils

import java.time.Instant
import java.util.UUID
import scala.jdk.CollectionConverters._
import com.typesafe.config.{ ConfigFactory, ConfigList, ConfigValue }
import play.api.{ ConfigLoader, Configuration }
import models.domain.PlatformCurrency

object SystemConfig {
	val config = ConfigFactory.load()
	val MAILER_ADDRESS: String = config.getString("play.mailer.user")
	val DEFAULT_HOST: String = "http://127.0.0.1:9000"
	// auto generated form the DB..
	var SUPPORTED_CURRENCIES: List[PlatformCurrency] = List()
	def COIN_USDC: PlatformCurrency = SUPPORTED_CURRENCIES.find(_.name == "usd-coin").getOrElse(null)
	def COIN_ETH: PlatformCurrency = SUPPORTED_CURRENCIES.find(_.name == "ethereum").getOrElse(null)
	def COIN_BTC: PlatformCurrency = SUPPORTED_CURRENCIES.find(_.name == "bitcoin").getOrElse(null)

	var DEFAULT_WEI_VALUE: BigDecimal = 0
	var DEFAULT_SYSTEM_SCHEDULER_TIMER: Int = 15
	var DEFAULT_EXPIRATION: Int = 15

	var NODE_SERVER_URI: String = ""
	var SCALA_SERVER_URI: String = ""
	var COINICA_WEB_HOST: String = ""
	var MAILER_HOST: String = ""
}