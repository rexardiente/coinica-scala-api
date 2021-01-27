package models.dao

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import cats.data.OptionT
import cats.implicits._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import ejisan.scalauthx.HashedCredential
import ejisan.kuro.otp.OTPKey
import models.domain._
import models.domain.enum.Roles
import utils.auth._

@Singleton
final class AdminDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class AdminTable(tag: Tag) extends Table[(Admin, Security)](tag, "ADMIN") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def email = column[String] ("EMAIL")
    def role = column[Roles.Role] ("ROLE")
    def createdAt = column[Instant] ("CREATED_AT")
    def admin = (id, email, role, createdAt) <> (Admin.tupled, Admin.unapply)

    def hashedPassword = column[HashedCredential]("HASHED_PASSWORD")
    def verificationInitialTime = column[Option[Instant]]("VERIFICATION_INITIAL_TIME")
    def resetPasswordCode = column[Option[OTPKey]]("RESET_PASSWORD_CODE")
    def newEmail = column[Option[String]]("NEW_EMAIL")
    def newEmailCode = column[Option[OTPKey]]("NEW_EMAIL_CODE")
    def disabledAt = column[Option[Instant]]("DISABLED_AT")

    def security = (
      hashedPassword,
      verificationInitialTime,
      resetPasswordCode,
      newEmail,
      newEmailCode,
      disabledAt) <> (Security.tupled, Security.unapply)

    def * = (admin, security)
  }

  object Query extends TableQuery(new AdminTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }

}

