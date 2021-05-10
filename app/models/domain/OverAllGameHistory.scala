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
	def user: String
	def bet: Double
	def toJson(): JsValue = Json.toJson(this)
}
// prediction default to WIN and after battle check if who wins to true else false for lose..
case class GQGameHistory(user: String, prediction: String, result: Boolean, bet: Double = 1) extends TransactionType {
	override def toJson(): JsValue = Json.toJson(this)
}
// prediction equals list of panels selected and check if `result == prediction` is win else lose
case class THGameHistory(user: String, prediction: List[Int], result: List[Int], bet: Double, amount: Double) extends TransactionType {
	override def toJson(): JsValue = Json.toJson(this)
}
// case class THGameHistory(user: String,
// 												destination: Int,
// 												enemy_count: Int,
// 												maxprize: Double,
// 												nextprize: Double,
// 												bet: Double,
// 												panel_set: Seq[THPanelSet],
// 												prize: Double,
// 												status: Int,
// 												unopentile: Int,
// 												win_count: Int) extends TransactionType {
// 	override def toJson(): JsValue = Json.toJson(this)
// }
// case class MJGameHistory(user: String, bet: Double, amount: Double) extends TransactionType
case class GameType(user: String, isWin: Boolean, bet: Double) extends TransactionType {
	override def toJson(): JsValue = Json.toJson(this)
}
case class PaymentType(user: String, data: String, bet: Double) extends TransactionType {
	require(data == "transfer" || data == "receive" , "Payment Type Input: invalid request")
	override def toJson(): JsValue = Json.toJson(this)
}
case class OverAllGameHistory(id: UUID,
															tx_hash: String,
															gameID: String, // name or ID
															game: String, // name or ID
															info: TransactionType,
															isConfirmed: Boolean, // update `confirmed` when system get notified from EOSIO net
															createdAt: Long) {
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

