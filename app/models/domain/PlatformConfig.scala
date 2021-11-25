package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._
import utils.CommonImplicits

object PlatformGame extends CommonImplicits
object PlatformHost extends CommonImplicits
object PlatformCurrency extends CommonImplicits
object PlatformConfig extends CommonImplicits {
	val tupled = (apply: (UUID,
												List[PlatformGame],
												List[PlatformHost],
												List[PlatformCurrency],
												String,
												Int,
												Int,
												Int,
												Instant) => PlatformConfig).tupled
}

// ghostquest = ["ghostquest", "0f335579-1bf8-4f9e-8ede-eb204f5c0cba", "GQ"]
// treasurehunt = ["treasurehunt", "1b977a2b-842e-430b-bd1b-c0bd3abe1c55", "TH"]
// mahjonghilo = ["mahjonghilo", "74cd374c-6126-495a-a8a3-33db87caa511", "MJ"]
// GQ.battle.timer = 15 will be move in others
case class PlatformGame(name: String,
												displayName: String,
												code: String,
												logo: String,
												path: String,
												genre: UUID,
												 others: JsValue,
 												description: String = "",
												id: UUID = UUID.randomUUID()) {
	def toJson(): JsValue = Json.toJson(this)
}
// val PROTOCOL: String = serverAllowedProtocols(1)
// val NODE_SERVER_URI: String = s"${PROTOCOL}://${serverAllowedURLs(1)}"
// val SCALA_SERVER_URI: String = s"${PROTOCOL}://${serverAllowedURLs(0)}"
// val COINICA_WEB_HOST: String = s"${PROTOCOL}://${serverAllowedURLs(2)}"
case class PlatformHost(name: String, uri: String, protocol: String = "https") {
	def toJson(): JsValue = Json.toJson(this)
	def getURL(): String = s"${protocol}://${uri}"
}
case class PlatformCurrency(name: String, symbol: String) {
	def toJson(): JsValue = Json.toJson(this)
}
// platform {
//   wei.value = "0.000000000000000001"
//   token.expiration = 15
//   # base currency must be set first on the list
//   # the rest are supported currencies
//   # be sure that supported.currencies and supported.symbols has same value
//   supported.currencies = ["usd-coin", "ethereum", "bitcoin"]
//   supported.symbols = ["USDC", "ETH", "BTC"]
//   games {
//     # [code, table, scope]
//     contracts {
//       ghostquest = ["ghostquest", "0f335579-1bf8-4f9e-8ede-eb204f5c0cba", "GQ"]
//       treasurehunt = ["treasurehunt", "1b977a2b-842e-430b-bd1b-c0bd3abe1c55", "TH"]
//       mahjonghilo = ["mahjonghilo", "74cd374c-6126-495a-a8a3-33db87caa511", "MJ"]
//     }
//     # in minute
//     GQ.battle.timer = 15
//   }
//   # in hour
//   default.system.scheduler = 24
// }
case class PlatformConfig(
					id: UUID,
					games: List[PlatformGame],
					hosts: List[PlatformHost],
					currencies: List[PlatformCurrency],
					wei: String = "0.000000000000000001",
					tokenExpiration: Int = 15,
					mailerExpiration: Int = 15,
					defaultscheduler: Int = 24,
					updatedAt: Instant = Instant.now) {
	def toJson(): JsValue = Json.toJson(this)
}

