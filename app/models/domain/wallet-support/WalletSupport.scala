package models.domain.wallet.support

import java.util.UUID
import java.time.Instant
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
case class CoinDeposit(txHash: String, issuer: Coin, receiver: Coin) {
	def toJson(): JsValue = Json.toJson(this)
	def toWalletHistory(id: UUID, txType: String, data: CryptoJsonRpcHistory): UserAccountWalletHistory =
		new UserAccountWalletHistory(txHash, id, receiver.currency, txType, data, Instant.now)
}

object CoinWithdraw extends utils.CommonImplicits
case class CoinWithdraw(receiver: Coin, fee: Long) {
	def toJson(): JsValue = Json.toJson(this)
	def toWalletHistory(txHash: String, id: UUID, txType: String, data: CryptoJsonRpcHistory): UserAccountWalletHistory =
		new UserAccountWalletHistory(txHash, id, receiver.currency, txType, data, Instant.now)
}