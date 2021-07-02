package models.domain

import java.time.Instant
import java.util.UUID
import play.api.libs.json._

object GameType
object PaymentType
object GQGameHistory
object THGameHistory
object TransactionType extends utils.CommonImplicits
object OverAllGameHistory extends utils.CommonImplicits

sealed trait TransactionType {
	def user: UUID
	def bet: Double
	def toJson(): JsValue = Json.toJson(this)
}
// prediction default to WIN and after battle check if who wins to true else false for lose..
case class GQGameHistory(user: UUID, prediction: String, result: Boolean, bet: Double = 1) extends TransactionType {
	override def toJson(): JsValue = Json.toJson(this)
}
// prediction equals list of panels selected and check if `result == prediction` is win else lose
case class THGameHistory(user: UUID, prediction: List[Int], result: List[Int], bet: Double, amount: Double) extends TransactionType {
	override def toJson(): JsValue = Json.toJson(this)
}
// case class MJGameHistory(user: String, bet: Double, amount: Double) extends TransactionType
case class GameType(user: UUID, isWin: Boolean, bet: Double) extends TransactionType {
	override def toJson(): JsValue = Json.toJson(this)
}
case class PaymentType(user: UUID, data: String, bet: Double) extends TransactionType {
	require(data == "transfer" || data == "receive" , "Payment Type Input: invalid request")
	override def toJson(): JsValue = Json.toJson(this)
}
case class OverAllGameHistory(id: UUID,
															txHash: String,
															gameID: String, // name or ID
															game: String, // name or ID
															info: TransactionType,
															isConfirmed: Boolean, // update `confirmed` when system get notified from EOSIO net
															createdAt: Long) {
	def toJson(): JsValue = Json.toJson(this)
}