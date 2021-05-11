package models.domain.multi.currency

import play.api.libs.json._

object Coin extends utils.CommonImplicits
case class Coin(symbol: String, name: String, isActive: Boolean) {
	def toJson(): JsValue = Json.toJson(this)
}

object Limits extends utils.CommonImplicits
case class Limits(depositCoin: String,
									destinationCoin: String,
									depositCoinMinAmount: BigDecimal,
									depositCoinMaxAmount: BigDecimal) {
	def toJson(): JsValue = Json.toJson(this)
}

object GenerateOffer extends utils.CommonImplicits
case class GenerateOffer(depositCoin: String,
												destinationCoin: String,
												depositCoinAmount: BigDecimal,
												destinationCoinAmount: BigDecimal,
												offerReferenceId: String) {
	def toJson(): JsValue = Json.toJson(this)
}

object WalletAddress
object CreateOrderTx
object CreateOrder extends utils.CommonImplicits
object CreateOrderResponse extends utils.CommonImplicits
case class WalletAddress(address: String, tag: Option[String])
case class CreateOrderTx(depositCoin: String,
												destinationCoin: String,
												depositCoinAmount: BigDecimal,
												destinationAddress: WalletAddress,
												refundAddress: WalletAddress,
												userReferenceId: String,
												offerReferenceId: String)
case class CreateOrder(transaction: CreateOrderTx) {
	def toJson(): JsValue = Json.toJson(this)
}
case class CreateOrderResponse(orderId: String, exchangeAddress: WalletAddress) {
	def toJson(): JsValue = Json.toJson(this)
}

object OrderStatus extends utils.CommonImplicits
object ListOfOrders extends utils.CommonImplicits
case class OrderStatus(orderId: String,
											exchangeAddress: WalletAddress,
											destinationAddress: WalletAddress,
											createdAt: Long,
											status: String,
											inputTransactionHash: Option[String],
											outputTransactionHash: Option[String],
											depositCoin: String,
											destinationCoin: JsValue,
											depositCoinAmount: Option[BigDecimal],
											destinationCoinAmount: Option[BigDecimal],
											validTill: String,
											userReferenceId: String) {
	def toJson(): JsValue = Json.toJson(this)
}
case class ListOfOrders(count: Int, items: Seq[OrderStatus]) {
	def toJson(): JsValue = Json.toJson(this)
}
object KeyPairGeneratorResponse extends utils.CommonImplicits
case class KeyPairGeneratorResponse(currency: String, address: String, privateKey: String, publicKey: String) {
	def toJson(): JsValue = Json.toJson(this)
}