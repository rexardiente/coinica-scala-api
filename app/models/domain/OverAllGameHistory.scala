package models.domain

import java.time.Instant
import java.util.UUID
import play.api.libs.json._

object GameType
object PaymentType
object BooleanPredictions
object IntPredictions
object ListOfIntPredictions
object TransactionType extends utils.CommonImplicits
object OverAllGameHistory extends utils.CommonImplicits

sealed trait TransactionType {
	def user(): String
	def bet(): Double
	def amount(): Double
	def toJson(): JsValue = Json.toJson(this)
}
// prediction default to WIN and after battle check if who wins to true else false for lose..
case class BooleanPredictions(user: String, prediction: Boolean, result: Boolean, bet: Double, amount: Double, more_info: Option[JsValue]) extends TransactionType {
	override def toJson(): JsValue = Json.toJson(this)
	def isWin(): Boolean = (prediction == result)
}
case class IntPredictions(user: String, prediction: Int, result: Int, bet: Double, amount: Double, more_info: Option[JsValue]) extends TransactionType {
	override def toJson(): JsValue = Json.toJson(this)
	def isWin(): Boolean = (prediction == result)
}
// prediction equals list of panels selected and check if `result == prediction` is win else lose
case class ListOfIntPredictions(user: String, prediction: List[Int], result: List[Int], bet: Double, amount: Double, more_info: Option[JsValue]) extends TransactionType {
	override def toJson(): JsValue = Json.toJson(this)
	def isWin(): Boolean = (prediction == result)
}
// case class MJGameHistory(user: String, bet: Double, amount: Double) extends TransactionType
case class GameType(user: String, isWin: Boolean, bet: Double) extends TransactionType {
	override def toJson(): JsValue = Json.toJson(this)
	def amount(): Double = 0D
}
case class PaymentType(user: String, data: String, amount: Double) extends TransactionType {
	require(data == "WITHDRAW" || data == "DEPOSIT", "Payment Type Input: invalid request")
	override def toJson(): JsValue = Json.toJson(this)
	def bet(): Double = 0D
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