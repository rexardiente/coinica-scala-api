package models.domain.wallet.support

import java.util.UUID
import java.time.Instant
import play.api.libs.json._

object FailedCoinDeposit extends utils.CommonImplicits {
	val tupled = (apply: (String, UUID, Coin, Coin, Instant) => FailedCoinDeposit).tupled
	def apply(tx_hash: String, id: UUID, issuer: Coin, receiver: Coin): FailedCoinDeposit =
		new FailedCoinDeposit(tx_hash, id, issuer, receiver, Instant.now)
}
case class FailedCoinDeposit(tx_hash: String, id: UUID, issuer: Coin, receiver: Coin, created_at: Instant) {
	def toJson(): JsValue = Json.toJson(this)
}