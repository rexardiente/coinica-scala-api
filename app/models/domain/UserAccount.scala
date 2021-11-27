package models.domain

import java.time.Instant
import java.util.UUID
import play.api.libs.json._
import models.domain.wallet.support.Coin
import utils.CommonImplicits

object UserAccount extends CommonImplicits {
	val tupled = (apply: (UUID,
												Int,
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
						userGameID: Int,
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
										userGameID,
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
						0,
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
						0,
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
											userGameID: Int,
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

object UserAccountWallet extends CommonImplicits {
	val tupled = (apply: (UUID, List[Coin]) => UserAccountWallet).tupled
}
case class UserAccountWallet(id: UUID, wallet: List[Coin]) {
	def toJson(): JsValue = Json.toJson(this)
}