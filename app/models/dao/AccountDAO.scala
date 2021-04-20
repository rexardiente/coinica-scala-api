package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.Account

@Singleton
final class AccountDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class AccountTable(tag: Tag) extends Table[Account](tag, "ACCOUNT") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def username = column[String] ("USERNAME")
    def password = column[Option[String]] ("PASSWORD")
    def createdAt = column[Long] ("CREATED_AT")

   def * = ( id,username, password, createdAt) <> ((Account.apply _).tupled, Account.unapply)
  }

  object Query extends TableQuery(new AccountTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }

}