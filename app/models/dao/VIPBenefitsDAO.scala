package models.dao

import java.time.Instant
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import ejisan.scalauthx.HashedCredential
import models.domain.VIPBenefits
import models.domain.enum._

@Singleton
final class VIPBenefitsDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class VIPBenefitsTable(tag: Tag) extends Table[VIPBenefits](tag, "VIP_BENEFITS") {
    def id = column[VIP.value] ("ID", O.PrimaryKey)
    def cashBack = column[Double] ("CASH_BACK")
    def redemptionRate = column[Double] ("REDEMPTION_RATE")
    def referralRate = column[Double] ("REFERRAL_RATE")
    def closedBeta = column[Boolean] ("CLOSED_BETA")
    def concierge = column[Boolean] ("CONCIERGE")
    def amount = column[VIPBenefitsAmount.value] ("AMOUNT")
    def points = column[VIPBenefitsPoints.value] ("POINTS")
    def updatedAt = column[Instant] ("UPDATED_AT")

    def * = (id,
    		cashBack,
    		redemptionRate,
    		referralRate,
    		closedBeta,
    		concierge,
    		amount,
    		points,
    		updatedAt) <> (VIPBenefits.tupled, VIPBenefits.unapply)
  }

  object Query extends TableQuery(new VIPBenefitsTable(_)) {
    def apply(id: VIP.value) = this.withFilter(_.id === id)
  }
}