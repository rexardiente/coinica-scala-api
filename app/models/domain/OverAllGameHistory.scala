package models.domain

import java.time.Instant
import java.util.UUID
import play.api.libs.json._

object GameType
object PaymentType
object GQGameHistory
object THGameHistory
object TransactionType extends utils.CommonImplicits
object OverAllGameHistory { implicit def implOverAllGameHistory = Json.format[OverAllGameHistory] }

sealed trait TransactionType {
	def toJson(): JsValue = Json.toJson(this)
}
// prediction default to WIN and after battle check if who wins to 1 else 0 for lose..
case class GQGameHistory(name: String, prediction: String, result: Int, bet: Double) extends TransactionType {
	override def toJson(): JsValue = Json.toJson(this)
}
// prediction equals list of panels selected and check if `result == prediction` is win else lose
case class THGameHistory(name: String, prediction: List[Int], result: List[Int], bet: Double) extends TransactionType {
	override def toJson(): JsValue = Json.toJson(this)
}
case class GameType(name: String, isWin: Boolean, amount: Double) extends TransactionType {
	override def toJson(): JsValue = Json.toJson(this)
}
case class PaymentType(name: String, data: String, amount: Double) extends TransactionType {
	require(data == "transfer" || data == "receive" , "Payment Type Input: invalid request")
	override def toJson(): JsValue = Json.toJson(this)
}
case class OverAllGameHistory(id: UUID,
															gameID: UUID, // name or ID
															game: String, // name or ID
															`type`: TransactionType,
															isConfirmed: Boolean, // update `confirmed` when system get notified from EOSIO net
															createdAt: Instant) {
	def toJson(): JsValue = Json.toJson(this)
}
// GAME TRANSACTION
// 	- TRANSACTION TYPE
// 		- GAME
// 		- PAYMENT AS TRANSFER (FROM TO ACCOUNT)

// - GAME
// 	- ACCOUNT/USERNAME
// 	- IS_WIN
// 	- AMOUNT

// - PAYMENT AS TRANSFER (FROM TO ACCOUNT)
// 	- ACCOUNT/USERNAME
// 	- TRANSFER OR RECEIVED
// 	- AMOUNT


// TX ID | GAME 	  | TYPE 		| PLAYER| RESULT 	 | AMOUNT
// 1 		| MJ		  | GAME 		| user1 | WIN 		 | 3.23
// 2 		| TH		  | GAME 		| user2 | LOSE		 | 2.34
// 3 		| TH  		| PAYMENT | user1 | TRANSFER | 1.00
// 4 		| MJ 		  | PAYMENT | user3 | RECEIVED | 0.90

