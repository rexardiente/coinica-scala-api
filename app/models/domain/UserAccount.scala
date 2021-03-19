package models.domain

import java.time.Instant
import java.util.UUID
import play.api.libs.json._

object UserAccount {
	val tupled = (apply: (UUID, String, Option[String], String, Double, Double, Double, Instant) => UserAccount).tupled
	def apply(id: UUID,
						name: String,
						referred_by: Option[String],
						referral_code: String,
						referral: Double,
						referral_rate: Double,
						win_rate: Double, // TODO
						created_at: Instant): UserAccount =
		new UserAccount(id, name, referred_by, referral_code, referral, referral_rate, win_rate, created_at)
	def apply(name: String): UserAccount = new UserAccount(UUID.randomUUID, name, None, UUID.randomUUID.toString.replaceAll("-", ""))
	implicit def implUserAccount = Json.format[UserAccount]
}
// TODO: Win rate and referral system
// 2.0 as defult referral rate
// Referral Code: check if user has already referral history then invalid request
case class UserAccount(id: UUID,
											name: String,
											referred_by: Option[String],
											referral_code: String,
											referral: Double = 0,
											referral_rate: Double = 2,
											win_rate: Double = 0,
											created_at: Instant = Instant.now)