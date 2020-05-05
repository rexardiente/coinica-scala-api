package models.dao.user.info

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.user.info.User

@Singleton
final class UserDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  protected class UserTable(tag: Tag) extends Table[User](tag, "USER") {
    def id = column[UUID] ("ID")
    def account = column[String] ("ACCOUNT")
    def address = column[String] ("ADDRESS")
    def createdAt = column[Instant] ("CREATED_AT")

   def * = (id, account, address, createdAt) <> ((User.apply _).tupled, User.unapply)
  }

  object Query extends TableQuery(new UserTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}

