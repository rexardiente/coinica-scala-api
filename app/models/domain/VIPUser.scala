package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import models.domain.enum.VIP

object VIPUser extends utils.CommonImplicits {
	val tupled = (apply: (UUID, VIP.value, VIP.value, Int, Double, Double, Instant) => VIPUser).tupled
	def apply(id: UUID,
						rank: VIP.value,
						next_rank: VIP.value,
						referral_count: Int,
						payout: Double,
						points: Double,
						updated_at: Instant): VIPUser =
    new VIPUser(id, rank, next_rank, referral_count, payout, points, updated_at)
  def apply(user: UUID, createdAt: Instant): VIPUser =
  	new VIPUser(user, VIP.BRONZE, VIP.BRONZE, 0, 0, 0, createdAt)
}
case class VIPUser(id: UUID,
									rank: VIP.value,
									next_rank: VIP.value,
									referral_count: Int,
									payout: Double,
									points: Double,
									updated_at: Instant) {
	private val pointsToInt: Int = points.round.toInt

	def toJson(): JsValue = Json.toJson(this)
	def currentLvlMax(): Double = { if (0 to 50 contains pointsToInt) 50 else if (51 to 150 contains pointsToInt) 150 else 1000 }.toDouble
	def prevLvlMax(): Double = { if (0 to 50 contains pointsToInt) 0 else if (51 to 150 contains pointsToInt) 50 else 150 }.toDouble
	def nextLvlMax(): Double = { if (0 to 50 contains pointsToInt) 150 else 1000 }.toDouble
	def currentRank(): VIP.value = if (0 to 50 contains pointsToInt) VIP.BRONZE else if (51 to 150 contains pointsToInt) VIP.SILVER else VIP.GOLD
	def nextRank(): VIP.value = if (0 to 50 contains pointsToInt) VIP.SILVER else VIP.GOLD
}
// VIP Percentage calculations:
// 0 - 50 = Bronze
// 51 - 150 = Silver
// 251 and up  = Gold
// sample current_points = 110.300000
// 110.300000 / 150 * 50 = 22.12% out of 50%
