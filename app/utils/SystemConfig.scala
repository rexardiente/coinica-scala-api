package utils

import java.time.{ Instant, LocalTime, LocalDate, LocalDateTime, ZoneOffset }
import java.util.UUID
import scala.jdk.CollectionConverters._
import com.typesafe.config.{ ConfigFactory, ConfigList, ConfigValue }
import play.api.libs.json._
import play.api.{ ConfigLoader, Configuration }
import models.domain.{ Genre, PlatformCurrency, PlatformGame, PlatformHost }

object SystemConfig {
	val config = ConfigFactory.load()
	val MAILER_ADDRESS: String = config.getString("play.mailer.user")
	val DEFAULT_HOST: String = "http://127.0.0.1:9000"
	// Initial DB data
	val defaultGenre: Genre = new Genre(UUID.randomUUID(), "LUCKY", Some("Play with your luck."))
	val gqDescription: String = """Idle game with character-battle. You need only to draw loot box and summon ghosts and send them in battle field. If your ghost wins, you can get rewrard."""
	val thDescription: String = """Seeking for treasure in three different ocean maps without being spotted by rival pirates!"""
	val mjDescription: String = """High-low game with mahjong tiles, if you can complete a hand with 14 tiles until 33rd turn, you get a bonus according to your hand score ranking."""
	// ghostquest = ["ghostquest", "0f335579-1bf8-4f9e-8ede-eb204f5c0cba", "GQ"]
	// treasurehunt = ["treasurehunt", "1b977a2b-842e-430b-bd1b-c0bd3abe1c55", "TH"]
	// mahjonghilo = ["mahjonghilo", "74cd374c-6126-495a-a8a3-33db87caa511", "MJ"]
	// GQ.battle.timer = 15 will be move in others
	val initialGames = List(new PlatformGame("ghostquest",
	                                  "GHOST QUEST",
	                                  "GQ",
	                                  "https://egs-2.s3.jp-tok.cloud-object-storage.appdomain.cloud/eos-web/imgs/platform/games/GQ.jpeg",
	                                  "/game/ghostquest",
	                                  defaultGenre.id,
	                                  Json.obj("battle_timer" -> 15),
	                                  gqDescription),
	                new PlatformGame("treasurehunt",
	                                  "TREASURE HUNT",
	                                  "TH",
	                                  "https://egs-2.s3.jp-tok.cloud-object-storage.appdomain.cloud/eos-web/imgs/platform/games/TH.jpeg",
	                                  "/game/treasurehunt",
	                                  defaultGenre.id,
	                                  JsNull,
	                                  thDescription),
	                new PlatformGame("mahjonghilo",
	                                  "MAHJONG HILO",
	                                  "MJ",
	                                  "https://egs-2.s3.jp-tok.cloud-object-storage.appdomain.cloud/eos-web/imgs/platform/games/MJ.png",
	                                  "/game/mahjong",
	                                  defaultGenre.id,
	                                  JsNull,
	                                  mjDescription))
	val initialHosts = List(new PlatformHost("scala_api", "api.coinica.net/s1"),
	                new PlatformHost("node_api", "api.coinica.net/s2"),
	                new PlatformHost("coinica", "coinica.net"),
	                new PlatformHost("mailer", "api.coinica.net/s1"))
	val initialCurrencies = List(new PlatformCurrency("usd-coin", "USDC"),
	                      new PlatformCurrency("ethereum", "ETH"),
	                      new PlatformCurrency("bitcoin", "BTC"))

	// System Timezone
	val defaultTimeZone: ZoneOffset = ZoneOffset.UTC
	def timeNowUTC(): LocalDateTime = LocalDateTime.ofInstant(Instant.now, defaultTimeZone)
	def instantNowUTC(): Instant = LocalDateTime.ofInstant(Instant.now, defaultTimeZone).toInstant(defaultTimeZone)
	def startOfDayUTC(): Instant = LocalDate.now(defaultTimeZone).atStartOfDay().toInstant(defaultTimeZone)
	def dateNowPlusDaysUTC(days: Int): Instant = LocalDate.now(defaultTimeZone).atStartOfDay().plusDays(days).toInstant(defaultTimeZone)
	// values will be regenerated form the DB
	var SUPPORTED_CURRENCIES: List[PlatformCurrency] = initialCurrencies
	var DEFAULT_WEI_VALUE: BigDecimal = 0
	var DEFAULT_SYSTEM_SCHEDULER_TIMER: Int = 24
	var DEFAULT_EXPIRATION: Int = 15
	var NODE_SERVER_URI: String = initialHosts(1).name
	var SCALA_SERVER_URI: String = initialHosts(0).name
	var COINICA_WEB_HOST: String = initialHosts(2).name
	var MAILER_HOST: String = initialHosts(3).name
}