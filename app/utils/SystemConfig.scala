package utils

import java.time.{ Instant, LocalTime, LocalDate, LocalDateTime, ZoneOffset }
import java.util.UUID
import scala.jdk.CollectionConverters._
import com.typesafe.config.{ ConfigFactory, ConfigList, ConfigValue }
import play.api.{ ConfigLoader, Configuration }
import models.domain.PlatformCurrency

object SystemConfig {
	val config = ConfigFactory.load()
	val MAILER_ADDRESS: String = config.getString("play.mailer.user")
	val DEFAULT_HOST: String = "http://127.0.0.1:9000"
	// System Timezone
	val defaultTimeZone: ZoneOffset = ZoneOffset.UTC
	def timeNowUTC(): LocalDateTime = LocalDateTime.ofInstant(Instant.now, defaultTimeZone)
	def instantNowUTC(): Instant = LocalDateTime.ofInstant(Instant.now, defaultTimeZone).toInstant(defaultTimeZone)
	def startOfDayUTC(): Instant = LocalDate.now(defaultTimeZone).atStartOfDay().toInstant(defaultTimeZone)
	def dateNowPlusDaysUTC(days: Int): Instant = LocalDate.now(defaultTimeZone).atStartOfDay().plusDays(days).toInstant(defaultTimeZone)
	// auto generated form the DB..
	var SUPPORTED_CURRENCIES: List[PlatformCurrency] = List()
	var DEFAULT_WEI_VALUE: BigDecimal = 0
	var DEFAULT_SYSTEM_SCHEDULER_TIMER: Int = 24
	var DEFAULT_EXPIRATION: Int = 15

	var NODE_SERVER_URI: String = ""
	var SCALA_SERVER_URI: String = ""
	var COINICA_WEB_HOST: String = ""
	var MAILER_HOST: String = ""
}