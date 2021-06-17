package models.domain.wallet.support

import java.util.UUID
import play.api.libs.json._

object Coin extends utils.CommonImplicits {
	def apply(address: Option[String], currency: String, amount: Double): Coin = new Coin(address, currency, amount)
	def apply(currency: String, amount: Double): Coin = Coin(None, currency, amount)
	def apply(currency: String): Coin = Coin(currency, 0)
}
case class Coin(address: Option[String], currency: String, amount: Double) {
	def toJson(): JsValue = Json.toJson(this)
}

object CoinDeposit extends utils.CommonImplicits
case class CoinDeposit(id: UUID, txHash: String, issuer: Coin, receiver: Coin) {
	def toJson(): JsValue = Json.toJson(this)
}

object CoinWithdraw extends utils.CommonImplicits
case class CoinWithdraw(id: UUID, currency: String, receiver: Coin) {
	def toJson(): JsValue = Json.toJson(this)
}
