package models.dao.login

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.login.Login

@Singleton
final class LoginDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  protected class LoginTable(tag: Tag) extends Table[Login](tag, "LOGIN") {
    def id = column[UUID] ("ID")
    def emailaddress = column[String] ("emailaddress")
    def password = column[String] ("PASSWORD")
    def logincreated = column[Instant] ("LOGINCREATED")

   def * = (id, emailaddress, password, logincreated) <> ((Login.apply _).tupled, Login.unapply)
  }

  object Query extends TableQuery(new LoginTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}