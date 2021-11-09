package models.dao

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.{ UserAccount, VIPUser, UserAccountWallet }
import models.domain.wallet.support.{ Coin, FailedCoinDeposit }
import models.domain.enum._
import utils.SystemConfig.SUPPORTED_SYMBOLS

@Singleton
final class UserAccountDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class UserAccountTable(tag: Tag) extends Table[UserAccount](tag, "USER_ACCOUNT_INFO") {
    def id = column[UUID]("ID", O.PrimaryKey)
    def userGameID = column[Int]("USER_GAME_ID", O.AutoInc)
    def username = column[String]("USERNAME")
    def password = column[String]("PASSWORD")
    def email = column[Option[String]]("EMAIL_ADDRESS")
    def referredBy = column[Option[String]]("REFERRED_BY")
    def referralCode = column[String]("REFERRAL_CODE")
    def referralAmount = column[Double]("REFERRAL_AMOUNT")
    def referralRate = column[Double]("REFERRAL_RATE")
    def winRate = column[Double]("WIN_RATE")
    def isVerified = column[Boolean]("IS_VERIFIED")
    def lastSignIn = column[Instant]("LAST_SIGN_IN")
    def createdAt = column[Instant]("CREATED_AT")

    def * = (id,
            userGameID,
            username,
            password,
            email,
            referredBy,
            referralCode,
            referralAmount,
            referralRate,
            winRate,
            isVerified,
            lastSignIn,
            createdAt) <> (UserAccount.tupled, UserAccount.unapply)
  }
  protected class VIPUserTable(tag: Tag) extends Table[VIPUser](tag, "USER_ACCOUNT_VIP") {
    def id = column[UUID]("ID", O.PrimaryKey)
    def rank = column[VIP.value]("RANK")
    def nxtRank = column[VIP.value]("NEXT_RANK")
    def referralCount = column[Int]("REFERRAL_COUNT")
    def payout = column[Double]("PAYOUT")
    def points = column[Double]("POINTS")
    def createdAt = column[Instant]("UPDATED_AT")

    def * = (id, rank, nxtRank, referralCount, payout, points, createdAt) <> (VIPUser.tupled, VIPUser.unapply)
    def fk = foreignKey("USER_ACCOUNT_INFO", id, UserAccountQuery)(_.id)
  }
  protected class UserAccountWalletTable(tag: Tag) extends Table[UserAccountWallet](tag, "USER_ACCOUNT_WALLET") {
    def id = column[UUID]("ID", O.PrimaryKey)
    def btc = column[Coin](SUPPORTED_SYMBOLS(2))
    def eth = column[Coin](SUPPORTED_SYMBOLS(1))
    def usdc = column[Coin](SUPPORTED_SYMBOLS(0))

    def * = (id, btc, eth, usdc) <> (UserAccountWallet.tupled, UserAccountWallet.unapply)
    def fk = foreignKey("USER_ACCOUNT_INFO", id, UserAccountQuery)(_.id)
  }

  protected class FailedCoinDepositTable(tag: Tag) extends Table[FailedCoinDeposit](tag, "FAILED_COIN_DEPOSIT") {
    def txHash = column[String] ("TX_HASH", O.PrimaryKey)
    def id = column[UUID] ("ACCOUNT_ID")
    def issuer = column[Coin] ("ISSUER")
    def receiver = column[Coin] ("RECEIVER")
    def createdAt = column[Instant] ("CREATED_AT")

    def * = (txHash, id, issuer, receiver, createdAt) <> (FailedCoinDeposit.tupled, FailedCoinDeposit.unapply)
    def fk = foreignKey("USER_ACCOUNT_INFO", id, UserAccountQuery)(_.id)
  }

  object UserAccountQuery extends TableQuery(new UserAccountTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
    def apply(username: String) = this.withFilter(_.username === username)
  }
  object VIPUserQuery extends TableQuery(new VIPUserTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
  object UserWalletQuery extends TableQuery(new UserAccountWalletTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
  object FailedCoinDepositQuery extends TableQuery(new FailedCoinDepositTable(_)) {
    def apply(txHash: String) = this.withFilter(_.txHash === txHash)
    def apply(createdAt: Instant) = this.withFilter(_.createdAt === createdAt)
  }
}

