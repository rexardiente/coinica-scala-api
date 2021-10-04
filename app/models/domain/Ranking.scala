package models.domain

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._

object RankType extends utils.CommonImplicits
object RankingHistory extends utils.CommonImplicits {
	val tupled = (apply: (UUID, Seq[RankType], Seq[RankType], Seq[RankType], Seq[RankType], Long) => RankingHistory).tupled
	def apply(id: UUID,
						profits: Seq[RankType],
						payouts: Seq[RankType],
						wagered: Seq[RankType],
						multipliers: Seq[RankType],
						created_at: Long): RankingHistory =
		new RankingHistory(id, profits, payouts, wagered, multipliers, created_at)
	def apply(profits: Seq[RankType],
						payouts: Seq[RankType],
						wagered: Seq[RankType],
						multipliers: Seq[RankType],
						created_at: Long): RankingHistory =
		new RankingHistory(UUID.randomUUID, profits, payouts, wagered, multipliers, created_at)
}
object RankProfit
object RankPayout
object RankWagered
object RankMultiplier
sealed trait RankType {
	def id: UUID
	def username: String
	def bet: Double
	def toJson(): JsValue = Json.toJson(this)
}
case class RankProfit(id: UUID, username: String, bet: Double, profit: Double) extends RankType {
	override def toJson(): JsValue = Json.toJson(this)
}
case class RankPayout(id: UUID, username: String, bet: Double, payout: Double) extends RankType {
	override def toJson(): JsValue = Json.toJson(this)
}
case class RankWagered(id: UUID, username: String, bet: Double, wagered: Double) extends RankType {
	override def toJson(): JsValue = Json.toJson(this)
}
case class RankMultiplier(id: UUID, username: String, bet: Double, multiplier: Double) extends RankType {
	override def toJson(): JsValue = Json.toJson(this)
}
case class RankingHistory(id: UUID,
													profits: Seq[RankType], // Seq[RankProfit]
													payouts: Seq[RankType], // Seq[RankPayout]
													wagered: Seq[RankType], // Seq[RankWagered]
													multipliers: Seq[RankType], // Seq[RankMultiplier]
													created_at: Long) {
	def toJson(): JsValue = Json.toJson(this)
}