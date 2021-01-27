package models.domain

import java.time.Instant
import models.domain.enum.{ VIP, VIPBenefitsAmount, VIPBenefitsPoints }

object VIPBenefits {
	val tupled = (apply: (VIP.value,
						Double,
						Double,
						Double,
						Boolean,
						Boolean,
						VIPBenefitsAmount.value,
						VIPBenefitsPoints.value,
						Instant) => VIPBenefits).tupled
	def apply(rank: VIP.value,
			cashBack: Double,
			redemptionRate: Double,
			referralRate: Double,
			closedBeta: Boolean,
			concierge: Boolean,
			amount: VIPBenefitsAmount.value,
			points: VIPBenefitsPoints.value,
			updatedAt: Instant): VIPBenefits =
    new VIPBenefits(rank, cashBack, redemptionRate, referralRate, closedBeta, concierge, amount, points, updatedAt)
}

case class VIPBenefits(
		rank: VIP.value,
		cashBack: Double,
		redemptionRate: Double,
		referralRate: Double,
		closedBeta: Boolean,
		concierge: Boolean,
		amount: VIPBenefitsAmount.value,
		points: VIPBenefitsPoints.value,
		updatedAt: Instant)