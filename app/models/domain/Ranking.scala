package models.domain

import java.util.UUID
import play.api.libs.json._
import utils.CommonImplicits

object RankType extends CommonImplicits
object RankingHistory extends CommonImplicits {
	val tupled = (apply: (UUID, Seq[RankType], Seq[RankType], Seq[RankType], Seq[RankType], Long) => RankingHistory).tupled
	def apply(id: UUID,
						profits: Seq[RankType],
						payouts: Seq[RankType],
						wagered: Seq[RankType],
						multipliers: Seq[RankType],
						createdAt: Long): RankingHistory =
		new RankingHistory(id, profits, payouts, wagered, multipliers, createdAt)
	def apply(profits: Seq[RankType],
						payouts: Seq[RankType],
						wagered: Seq[RankType],
						multipliers: Seq[RankType],
						createdAt: Long): RankingHistory =
		new RankingHistory(UUID.randomUUID, profits, payouts, wagered, multipliers, createdAt)
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
													createdAt: Long) {
	def toJson(): JsValue = Json.toJson(this)
}