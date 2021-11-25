package models.domain.wallet.support

import java.util.UUID
import java.time.Instant
import play.api.libs.json._

object Coin extends utils.CommonImplicits {
	def apply(address: Option[String], symbol: String, amount: BigDecimal): Coin = new Coin(address, symbol, amount)
	def apply(symbol: String, amount: BigDecimal): Coin = Coin(None, symbol, amount)
	def apply(symbol: String): Coin = Coin(symbol, 0)
}
case class Coin(address: Option[String], symbol: String, amount: BigDecimal) {
	def toJson(): JsValue = Json.toJson(this)
}

object CoinDeposit extends utils.CommonImplicits
case class CoinDeposit(txHash: String, issuer: Coin, receiver: Coin) {
	def toJson(): JsValue = Json.toJson(this)
	def toWalletHistory(id: UUID, txType: String, data: CryptoJsonRpcHistory): UserAccountWalletHistory =
		new UserAccountWalletHistory(txHash, id, receiver.symbol, txType, data, Instant.now)
}

object CoinWithdraw extends utils.CommonImplicits
case class CoinWithdraw(receiver: Coin, gasPrice: BigDecimal) {
	def toJson(): JsValue = Json.toJson(this)
	def toWalletHistory(txHash: String, id: UUID, txType: String, data: CryptoJsonRpcHistory): UserAccountWalletHistory =
		new UserAccountWalletHistory(txHash, id, receiver.symbol, txType, data, Instant.now)
}