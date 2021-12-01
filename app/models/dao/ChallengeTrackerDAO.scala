package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.ChallengeTracker

@Singleton
final class ChallengeTrackerDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class ChallengeTrackerTable(tag: Tag) extends Table[ChallengeTracker](tag, "CHALLENGE_TRACKER") {
    def user = column[UUID] ("USER", O.PrimaryKey)
    // def challengeID = column[UUID] ("CHALLENGE_ID")
    def bets = column[Double] ("BETS")
    def wagered = column[Double] ("WAGERED")
    def ratio = column[Double] ("RATIO")
    def points = column[Double] ("VIP_POINTS")
    def payout = column[Double] ("TOTAL_PAYOUT")
    def multiplier = column[Int] ("MULTIPLIERS")

    def * = (user, bets, wagered, ratio, points, payout, multiplier) <> (ChallengeTracker.tupled, ChallengeTracker.unapply)
  }

  object Query extends TableQuery(new ChallengeTrackerTable(_)) {
    def apply(user: UUID) = this.withFilter(_.user === user)
    def clearTbl = this.delete
  }
}