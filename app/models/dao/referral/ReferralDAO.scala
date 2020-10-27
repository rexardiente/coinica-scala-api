package models.dao.referral

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.referral.Referral

@Singleton
final class ReferralDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  protected class ReferralTable(tag: Tag) extends Table[Referral](tag, "REFERRAL") {
    def id = column[UUID] ("ID")
    def name = column[String] ("NAME")
    def imgURl = column[String] ("IMG_URL")
    def genre = column[UUID] ("GENRE")
    def description = column[Option[String]] ("DESCRIPTION")
    

     def * = (id, name, imgURl, genre, description)<>((Referral.apply _).tupled, Referral.unapply)
   
  }
 
  object Query extends TableQuery(new ReferralTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}