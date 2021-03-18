package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

// get overall history in 1 day
// and calculate who ranks in 24hrs (top 10)
// case class Ranking(id: UUID,
// 									name: String,
// 									bets: Double,
//  									profit: Double,
// 									multiplieramount: Double,
// 									rankingcreated: Long)
// object Ranking {
// 	implicit def implRanking = Json.format[Ranking]
// }

object RankType extends utils.CommonImplicits
object RankingHistory extends utils.CommonImplicits
object RankProfit
object RankPayout
object RankWagered
object RankMultiplier
sealed trait RankType {
	def user: UUID
	def bet: Double
	def toJson(): JsValue = Json.toJson(this)
}
case class RankProfit(user: UUID, bet: Double, profit: Double) extends RankType {
	override def toJson(): JsValue = Json.toJson(this)
}
case class RankPayout(user: UUID, bet: Double, payout: Double) extends RankType {
	override def toJson(): JsValue = Json.toJson(this)
}
case class RankWagered(user: UUID, bet: Double, wagered: Double) extends RankType {
	override def toJson(): JsValue = Json.toJson(this)
}
case class RankMultiplier(user: UUID, bet: Double, multiplier: Double) extends RankType {
	override def toJson(): JsValue = Json.toJson(this)
}
case class RankingHistory(id: UUID,
													profits: Seq[RankType], // Seq[RankProfit]
													payouts: Seq[RankType], // Seq[RankPayout]
													wagered: Seq[RankType], // Seq[RankWagered]
													multipliers: Seq[RankType], // Seq[RankMultiplier]
													created_at: Instant) {
	def toJson(): JsValue = Json.toJson(this)
}