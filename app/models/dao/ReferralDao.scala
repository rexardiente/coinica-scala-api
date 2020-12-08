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
    def referralname = column[String] ("REFFERALNAME")
    def referallink = column[String] ("REFERRALLINK")
    def rate = column[Double] ("RATE")
    def feeamount = column[Double] ("FEEAMOUNT")
    def referralcreated = column[Long] ("REFERRALCREATED")

    def * = (id, referralname,  referallink, rate,  feeamount, referralcreated) <> ((Referral.apply _).tupled, Referral.unapply) 
  }

  object Query extends TableQuery(new ReferralTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
 
}
