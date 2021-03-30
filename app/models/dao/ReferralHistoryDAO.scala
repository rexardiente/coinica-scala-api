package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.ReferralHistory

@Singleton
final class ReferralHistoryDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._


  protected class ReferralHistoryTable(tag: Tag) extends Table[ReferralHistory](tag, "REFERRAL_HISTORY") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def code = column[String] ("CODE")
    def appliedBy = column[UUID] ("APPLIED_BY")
    // def fee = column[Double] ("FEE")
    // def status = column[Boolean] ("STATUS")
    def createdAt = column[Instant] ("CREATED_AT")

    def * = (id, code,  appliedBy, createdAt) <> ((ReferralHistory.apply _).tupled, ReferralHistory.unapply)
  }

  object Query extends TableQuery(new ReferralHistoryTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }

}
