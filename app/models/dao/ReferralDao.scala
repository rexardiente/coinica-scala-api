package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.Referral

@Singleton
final class ReferralDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._


  protected class ReferralTable(tag: Tag) extends Table[Referral](tag, "REFERRAL") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def name = column[String] ("NAME")
    def gameID = column[UUID] ("GAME_ID") 
    def imgURl = column[String] ("IMG_URL")
    def amount = column[Double] ("AMOUNT")
    def referralcreated = column[Long] ("REFERRALCREATED")

    def * = (id, name, gameID, imgURl, amount, referralcreated) <> ((Referral.apply _).tupled, Referral.unapply) 
  }

  object Query extends TableQuery(new ReferralTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
 
}
