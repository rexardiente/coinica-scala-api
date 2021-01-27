package models.domain

import java.time.Instant
import models.domain.enum.{ VIP, VIPBenefitAmount, VIPBenefitPoints }

object VIPBenefit {
	val tupled = (apply: (VIP.value,
						Double,
						Double,
						Double,
						Boolean,
						Boolean,
						VIPBenefitAmount.value,
						VIPBenefitPoints.value,
						Instant) => VIPBenefit).tupled
	def apply(rank: VIP.value,
			cashBack: Double,
			redemptionRate: Double,
			referralRate: Double,
			closedBeta: Boolean,
			concierge: Boolean,
			amount: VIPBenefitAmount.value,
			points: VIPBenefitPoints.value,
			updatedAt: Instant): VIPBenefit =
    new VIPBenefit(rank, cashBack, redemptionRate, referralRate, closedBeta, concierge, amount, points, updatedAt)
}

case class VIPBenefit(
		rank: VIP.value,
		cashBack: Double,
		redemptionRate: Double,
		referralRate: Double,
		closedBeta: Boolean,
		concierge: Boolean,
		amount: VIPBenefitAmount.value,
		points: VIPBenefitPoints.value,
		updatedAt: Instant)