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
	def currentLvlMax(): Double = { if (0 to 200 contains pointsToInt) 200 else if (201 to 500 contains pointsToInt) 500 else 1000 }.toDouble
	def prevLvlMax(): Double = { if (0 to 200 contains pointsToInt) 0 else if (201 to 500 contains pointsToInt) 200 else 500 }.toDouble
	def nextLvlMax(): Double = { if (0 to 200 contains pointsToInt) 500 else 1000 }.toDouble
	def currentRank(): VIP.value = if (0 to 200 contains pointsToInt) VIP.BRONZE else if (201 to 500 contains pointsToInt) VIP.SILVER else VIP.GOLD
	def nextRank(): VIP.value = if (0 to 200 contains pointsToInt) VIP.SILVER else VIP.GOLD
}
// VIP Percentage calculations:
// 0 - 100 = Bronze
// 101 - 500 = Silver
// 501 and up  = Gold

// sample current_points = 110.600000
// 110.600000 / 500 * 100 = 22.12% out of 100%
