package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.Login
import scala.collection.mutable

@Singleton
final class LoginDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._


  protected class LoginTable(tag: Tag) extends Table[Login](tag, "LOGIN") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def username = column[String] ("USERNAME")
    def password = column[String] ("PASSWORD")
    def logincreated = column[Long] ("LOGINCREATED")

   def * = ( id,username, password, logincreated) <> ((Login.apply _).tupled, Login.unapply)
  }

  object Query extends TableQuery(new LoginTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }

}