package models.dao

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.UserTokens

@Singleton
final class UserTokensDAO @Inject()(
    userAccountDAO: UserAccountDAO,
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

    protected class UserTokensTable(tag: Tag) extends Table[UserTokens](tag, "USER_TOKENS") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def token = column[Option[String]] ("TOKEN")
    def login = column[Option[Long]] ("LOGIN")
    def email = column[Option[Long]] ("EMAIL")
    def password = column[Option[Long]] ("PASSWORD")

    def * = (id, token, login, email, password) <> (UserTokens.tupled, UserTokens.unapply)
    foreignKey("USER_ACCOUNT", id, userAccountDAO.Query)(_.id)
  }

  object Query extends TableQuery(new UserTokensTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}
