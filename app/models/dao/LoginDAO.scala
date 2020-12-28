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
  
    def username = column[String] ("USERNAME")
    def password = column[String] ("PASSWORD")
    

   def * = ( username, password) <> ((Login.apply _).tupled, Login.unapply)
  }

  object Query extends TableQuery(new LoginTable(_)) {
    def apply(username: String) = this.withFilter(_.username === username)
  }
  private val users = mutable.Map(
    "user001" -> Login("user001", "pass001")
  )

  def getUser(username: String): Option[Login] = {
    users.get(username)
  }
}