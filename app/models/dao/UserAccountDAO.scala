package models.dao

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.UserAccount

@Singleton
final class UserAccountDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  protected class UserAccountTable(tag: Tag) extends Table[UserAccount](tag, "USER_ACCOUNT") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def username = column[String] ("USERNAME")
    def password = column[String] ("PASSWORD")
    def email = column[Option[String]] ("EMAIL_ADDRESS")
    def referredBy = column[Option[String]] ("REFERRED_BY")
    def referralCode = column[String] ("REFERRAL_CODE")
    def referralAmount = column[Double] ("REFERRAL_AMOUNT")
    def referralRate = column[Double] ("REFERRAL_RATE")
    def winRate = column[Double] ("WIN_RATE")
    def isVerified = column[Boolean] ("IS_VERIFIED")
    def lastSignIn = column[Instant] ("LAST_SIGN_IN")
    def createdAt = column[Instant] ("CREATED_AT")

    def * = (id,
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

  object Query extends TableQuery(new UserAccountTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
    def apply(username: String) = this.withFilter(_.username === username)
  }
}

