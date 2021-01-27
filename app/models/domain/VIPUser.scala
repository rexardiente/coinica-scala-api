package models.domain

import java.time.Instant
import models.domain.enum.VIP

object VIPUser {
	val tupled = (apply: (String, VIP.value, VIP.value, Long, Long, Int, Instant) => VIPUser).tupled
	def apply(user: String,
						rank: VIP.value,
						nxtRank: VIP.value,
						payout: Long,
						points: Long,
						nextLvl: Int,
						updatedAt: Instant): VIPUser =
    new VIPUser(user, rank, nxtRank, payout, points, nextLvl, updatedAt)
}

case class VIPUser(
		user: String,
		rank: VIP.value,
		nxtRank: VIP.value, // estimated rank for netx month
		payout: Long,
		points: Long,
		nextLvl: Int, // percentage out of 100
		updatedAt: Instant)