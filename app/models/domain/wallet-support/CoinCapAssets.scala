package models.domain.wallet.support

import play.api.libs.json._

object CoinCapAsset extends utils.CommonImplicits
case class CoinCapAsset(id: String,
												rank: Int,
												symbol: String,
												name: String,
												supply: BigDecimal,
												maxSupply: BigDecimal,
												marketCapUsd: BigDecimal,
												volumeUsd24Hr: String,
												priceUsd: BigDecimal,
												changePercent24Hr: String,
												vwap24Hr: String) {
	def toJson(): JsValue = Json.toJson(this)
}