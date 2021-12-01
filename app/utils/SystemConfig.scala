package utils

import java.time.{ Instant, LocalTime, LocalDate, LocalDateTime, ZoneOffset }
import java.util.UUID
import scala.jdk.CollectionConverters._
// import scala.collection.JavaConverters.asScalaBufferConverter
import com.typesafe.config.{ ConfigFactory, ConfigList, ConfigValue }
import play.api.libs.json._
import play.api.{ ConfigLoader, Configuration }
import models.domain.{ Genre, PlatformCurrency, PlatformGame, PlatformHost }

object SystemConfig {
	val config = ConfigFactory.load()
	val initialGames: List[PlatformGame] = config.getConfigList("default.games")
			.asScala
			.toList
			.map { config =>
				val others = try { Json.parse(config.getString("others")) } catch { case _ :Throwable => JsNull }
				// if has battle_timer
	      PlatformGame(config.getString("name"),
						        config.getString("displayName"),
						        config.getString("code"),
						        config.getString("logo"),
						        config.getString("path"),
						        UUID.fromString(config.getString("genre")),
						        others,
						        config.getString("description"),
						        UUID.fromString(config.getString("id")))}
	val initialHosts: List[PlatformHost] = config.getList("default.hosts")
	 		.asScala
			.toList
			.map(configValue => configValue.unwrapped().asInstanceOf[java.util.ArrayList[String]])
			.map(_.asScala.toList)
			.map(value => PlatformHost(value(0).toString, value(1).toString))
	val initialCurrencies: List[PlatformCurrency] = config.getList("default.currencies")
	 		.asScala
			.toList
			.map(configValue => configValue.unwrapped().asInstanceOf[java.util.ArrayList[String]])
			.map(_.asScala.toList)
			.map(value => PlatformCurrency(value(0).toString, value(1).toString))

	val MAILER_ADDRESS: String = config.getString("play.mailer.user")
	val DEFAULT_HOST: String = "http://127.0.0.1:9000"

	var SUPPORTED_CURRENCIES: List[PlatformCurrency] = initialCurrencies
	var DEFAULT_WEI_VALUE: BigDecimal = BigDecimal(config.getString("default.wei"))
	var DEFAULT_SYSTEM_SCHEDULER_TIMER: Int = config.getInt("default.system.scheduler")
	var DEFAULT_EXPIRATION: Int = config.getInt("default.system.expiration")
	var NODE_SERVER_URI: String = initialHosts(1).uri
	var SCALA_SERVER_URI: String = initialHosts(0).uri
	var COINICA_WEB_HOST: String = initialHosts(2).uri
	var MAILER_HOST: String = initialHosts(3).uri

	// System Timezone
	val defaultTimeZone: ZoneOffset = ZoneOffset.UTC
	def timeNowUTC(): LocalDateTime = LocalDateTime.ofInstant(Instant.now, defaultTimeZone)
	def instantNowUTC(): Instant = LocalDateTime.ofInstant(Instant.now, defaultTimeZone).toInstant(defaultTimeZone)
	def startOfDayUTC(): Instant = LocalDate.now(defaultTimeZone).atStartOfDay().toInstant(defaultTimeZone)
	def dateNowPlusDaysUTC(days: Int): Instant = LocalDate.now(defaultTimeZone).atStartOfDay().plusDays(days).toInstant(defaultTimeZone)

}