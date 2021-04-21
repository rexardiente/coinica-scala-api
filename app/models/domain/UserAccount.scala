package models.domain

import java.time.Instant
import java.util.UUID
import play.api.libs.json._
import utils.auth.EncryptKey

object UserAccount extends utils.CommonImplicits {
	val tupled = (apply: (UUID,
												String,
												String,
												Option[String],
												Option[String],
												String,
												Double,
												Double,
												Double,
												Option[String],
												Option[Long],
												Boolean,
												Instant,
												Instant) => UserAccount).tupled
	def apply(id: UUID,
						username: String,
						password: String,
						email: Option[String],
						referredBy: Option[String],
						referralCode: String,
						referral: Double,
						referralRate: Double,
						winRate: Double,
						token: Option[String],
						tokenLimit: Option[Long],
						isVerified: Boolean,
						lastSignIn: Instant,
						createdAt: Instant): UserAccount =
		new UserAccount(id,
										username,
										password,
										email,
										referredBy,
										referralCode,
										referral,
										referralRate,
										winRate,
										token,
										tokenLimit,
										isVerified,
										lastSignIn,
										createdAt)
	// def apply(username: String): UserAccount = new UserAccount(UUID.randomUUID, username, None, UUID.randomUUID.toString.replaceAll("-", ""))
	def apply(username: String, password: String): UserAccount = new UserAccount(
						UUID.randomUUID,
						username,
						password,
						None,
						None,
						UUID.randomUUID.toString.replaceAll("-", ""),
						0,
						models.domain.enum.VIP.BRONZE.id,
						0,
						None,
						None,
						false,
						Instant.now,
						Instant.now)
	def apply(username: String, password: String, referredBy: Option[String]): UserAccount = new UserAccount(
						UUID.randomUUID,
						username,
						password,
						None,
						referredBy,
						UUID.randomUUID.toString.replaceAll("-", ""),
						0,
						models.domain.enum.VIP.BRONZE.id,
						0,
						None,
						None,
						false,
						Instant.now,
						Instant.now)
}
// TODO: Win rate and referral system
// 2.0 as defult referral rate
// Referral Code: check if user has already referral history then invalid request
case class UserAccount(id: UUID,
											username: String,
											password: String,
											email: Option[String],
											referredBy: Option[String],
											referralCode: String,
											referral: Double = 0,
											referralRate: Double = 0,
											winRate: Double = 0,
											token: Option[String],
											tokenLimit: Option[Long],
											isVerified: Boolean,
											lastSignIn: Instant,
											createdAt: Instant = Instant.now()) {
	def toJson(): JsValue = Json.toJson(this)
}

