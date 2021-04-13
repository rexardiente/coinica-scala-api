package models.domain

import java.time.Instant
import play.api.libs.json._
import models.domain.enum.{ VIP, VIPBenefitAmount, VIPBenefitPoints }

object VIPBenefit extends utils.CommonImplicits {
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
						cash_back: Double,
						redemption_rate: Double,
						referral_rate: Double,
						closed_beta: Boolean,
						concierge: Boolean,
						amount: VIPBenefitAmount.value,
						points: VIPBenefitPoints.value,
						updated_at: Instant): VIPBenefit =
    new VIPBenefit(rank, cash_back, redemption_rate, referral_rate, closed_beta, concierge, amount, points, updated_at)
}

case class VIPBenefit(
		rank: VIP.value = VIP.BRONZE,
		cash_back: Double = 0,
		redemption_rate: Double = 0,
		referral_rate: Double = 0,
		closed_beta: Boolean = false,
		concierge: Boolean = false,
		amount: VIPBenefitAmount.value = VIPBenefitAmount.BRONZE,
		points: VIPBenefitPoints.value = VIPBenefitPoints.BRONZE,
		updated_at: Instant = Instant.now) {
	def toJson(): JsValue = Json.toJson(this)
}