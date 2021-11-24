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
	def apply(id: VIP.value,
						cash_back: Double,
						redemption_rate: Double,
						referral_rate: Double,
						closed_beta: Boolean,
						concierge: Boolean,
						amount: VIPBenefitAmount.value,
						points: VIPBenefitPoints.value,
						updated_at: Instant): VIPBenefit =
	new VIPBenefit(id, cash_back, redemption_rate, referral_rate, closed_beta, concierge, amount, points, updated_at)
}

case class VIPBenefit(id: VIP.value,
											cash_back: Double,
											redemption_rate: Double,
											referral_rate: Double,
											closed_beta: Boolean,
											concierge: Boolean,
											amount: VIPBenefitAmount.value,
											points: VIPBenefitPoints.value,
											updated_at: Instant) {
	def toJson(): JsValue = Json.toJson(this)
}