package models.domain

import java.time.Instant
import java.util.UUID
import play.api.libs.json._

object UserAccount {
	val tupled = (apply: (UUID,
												String,
												String,
												Option[String],
												String,
												Double,
												Double,
												Double,
												Option[String],
												Option[Long],
												Instant,
												Instant) => UserAccount).tupled
	def apply(id: UUID,
						username: String,
						password: String,
						referred_by: Option[String],
						referral_code: String,
						referral: Double,
						referral_rate: Double,
						win_rate: Double, // TODO
						token: Option[String],
						tokenLimit: Option[Long],
						lastSignIn: Instant,
						created_at: Instant): UserAccount =
		new UserAccount(id,
										username,
										password,
										referred_by,
										referral_code,
										referral,
										referral_rate,
										win_rate,
										token,
										tokenLimit,
										lastSignIn,
										created_at)
	// def apply(username: String): UserAccount = new UserAccount(UUID.randomUUID, username, None, UUID.randomUUID.toString.replaceAll("-", ""))
	def apply(username: String, password: String): UserAccount = new UserAccount(
																														UUID.randomUUID,
																														username,
																														password,
																														None,
																														UUID.randomUUID.toString.replaceAll("-", ""),
																														0,
																														models.domain.enum.VIP.BRONZE.id,
																														0,
																														None,
																														None,
																														Instant.now,
																														Instant.now)
	def apply(username: String, password: String, referred_by: Option[String]): UserAccount = new UserAccount(
																														UUID.randomUUID,
																														username,
																														password,
																														referred_by,
																														UUID.randomUUID.toString.replaceAll("-", ""),
																														0,
																														models.domain.enum.VIP.BRONZE.id,
																														0,
																														None,
																														None,
																														Instant.now,
																														Instant.now)
	implicit def implUserAccount = Json.format[UserAccount]
}
// TODO: Win rate and referral system
// 2.0 as defult referral rate
// Referral Code: check if user has already referral history then invalid request
case class UserAccount(id: UUID,
											username: String,
											password: String,
											referred_by: Option[String],
											referral_code: String,
											referral: Double = 0,
											referral_rate: Double = 0,
											win_rate: Double = 0,
											token: Option[String], // TODO
											tokenLimit: Option[Long], // TODO
											lastSignIn: Instant, // TODO
											created_at: Instant = Instant.now()) {
	def toJson(): JsValue = Json.toJson(this)
}

