package models.dao

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.VIPBenefit
import models.domain.enum._

@Singleton
final class VIPBenefitDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class VIPBenefitsTable(tag: Tag) extends Table[VIPBenefit](tag, "VIP_BENEFITS") {
    def id = column[VIP.value] ("ID", O.PrimaryKey)
    def cashBack = column[Double] ("CASH_BACK")
    def redemptionRate = column[Double] ("REDEMPTION_RATE")
    def referralRate = column[Double] ("REFERRAL_RATE")
    def closedBeta = column[Boolean] ("CLOSED_BETA")
    def concierge = column[Boolean] ("CONCIERGE")
    def amount = column[VIPBenefitAmount.value] ("AMOUNT")
    def points = column[VIPBenefitPoints.value] ("POINTS")
    def updatedAt = column[Instant] ("UPDATED_AT")

    def * = (id,
        		cashBack,
        		redemptionRate,
        		referralRate,
        		closedBeta,
        		concierge,
        		amount,
        		points,
        		updatedAt) <> (VIPBenefit.tupled, VIPBenefit.unapply)
  }

  object Query extends TableQuery(new VIPBenefitsTable(_)) {
    def apply(id: VIP.value) = this.withFilter(_.id === id)
    def apply(cashBack: Double) = this.withFilter(_.cashBack === cashBack)
  }
}