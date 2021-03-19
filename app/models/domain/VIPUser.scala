package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import models.domain.enum.VIP

object VIPUser extends utils.CommonImplicits {
	val tupled = (apply: (UUID, String, VIP.value, VIP.value, Long, Long, Instant) => VIPUser).tupled
	def apply(id: UUID,
						user: String,
						rank: VIP.value,
						next_rank: VIP.value,
						payout: Long,
						points: Long,
						updated_at: Instant): VIPUser =
    new VIPUser(id, user, rank, next_rank, payout, points, updated_at)
}
case class VIPUser(id: UUID, // user_id
									user: String,
									rank: VIP.value,
									next_rank: VIP.value, // estimated rank for netx month
									payout: Long,
									points: Long,
									updated_at: Instant) {
	def toJson(): JsValue = Json.toJson(this)
}