package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.ChallengeTracker

@Singleton
final class ChallengeTrackerDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class ChallengeTrackerTable(tag: Tag) extends Table[ChallengeTracker](tag, "CHALLENGE_TRACKER") {
    def user = column[String] ("USER", O.PrimaryKey)
    def challengeID = column[UUID] ("CHALLENGE_ID")
    def bets = column[Double] ("BETS")
    def wagered = column[Double] ("WAGERED")
    def ratio = column[Double] ("RATIO")
    def points = column[Double] ("VIP_POINTS")

    def * = (user, challengeID, bets, wagered, ratio, points) <> (ChallengeTracker.tupled, ChallengeTracker.unapply)
  }

  object Query extends TableQuery(new ChallengeTrackerTable(_)) {
    def apply(user: String) = this.withFilter(_.user === user)
    def clearTbl = this.delete
  }
}