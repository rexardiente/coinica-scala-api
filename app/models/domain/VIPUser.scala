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
  	new VIPUser(user, VIP.BRONZE, VIP.SILVER, 0, 0, 0, createdAt)
}
case class VIPUser(id: UUID,
									rank: VIP.value,
									next_rank: VIP.value,
									referral_count: Int,
									payout: Double,
									points: Double,
									updated_at: Instant) {
	def toJson(): JsValue = Json.toJson(this)
	def toJson(progress: Double): JsValue = {
		Json.toJson(this).as[JsObject] + ("progress" -> JsString("%.2f".format(progress)))
	}
}
// VIP Percentage calculations:
// 0 - 50 = Bronze
// 51 - 150 = Silver
// 251 and up  = Gold
// sample current_points = 110.300000
// 110.300000 / 150 * 50 = 22.12% out of 50%
