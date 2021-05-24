package models.domain

import java.time.Instant
import java.util.UUID
import play.api.libs.json._
import utils.auth.EncryptKey
import models.domain.multi.currency.WalletKey

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
											isVerified: Boolean,
											lastSignIn: Instant,
											createdAt: Instant = Instant.now()) {
	def toJson(): JsValue = Json.toJson(this)
}

object UserToken extends utils.CommonImplicits {
	val tupled = (apply: (UUID, Option[String], Option[Long], Option[Long], Option[Long]) => UserToken).tupled
	def apply(id: UUID): UserToken = new UserToken(id, None, None, None, None)
}
case class UserToken(id: UUID,
										token: Option[String],
										login: Option[Long],
										email: Option[Long],
										password: Option[Long]) {
	def toJson(): JsValue = Json.toJson(this)
}

object UserAccountWallet extends utils.CommonImplicits {
	val tupled = (apply: (UUID, Double, Double, Double) => UserAccountWallet).tupled
}
case class UserAccountWallet(id: UUID, btc: Double, eth: Double, usdt: Double) {
	def toJson(): JsValue = Json.toJson(this)
}





