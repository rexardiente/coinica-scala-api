package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.ChallengeHistory

@Singleton
final class ChallengeHistoryDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class ChallengeHistoryTable(tag: Tag) extends Table[ChallengeHistory](tag, "CHALLENGE_HISTORY") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def challengeID = column[UUID] ("CHALLENGE_ID")
    def rank = column[Int] ("RANK")
    def name = column[String] ("NAME")
    def bet = column[Double] ("BET_AMOUNT")
    def profit = column[Double] ("PROFIT")
    def ratio = column[Double] ("RATIO")
    def vipPoints = column[Double] ("VIP_POINTS")
    def createdAt = column[Instant] ("CREATED_AT")

    def * = (id,
            challengeID,
            rank,
            name,
            bet,
            profit,
            ratio,
            vipPoints,
            createdAt) <> (ChallengeHistory.tupled, ChallengeHistory.unapply)
  }

  object Query extends TableQuery(new ChallengeHistoryTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}