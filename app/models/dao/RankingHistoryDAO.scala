package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.{ RankingHistory, RankType }

@Singleton
final class RankingHistoryDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class RankingHistoryTable(tag: Tag) extends Table[RankingHistory](tag, "RANKING_HISTORY") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def profits = column[Seq[RankType]] ("PROFITS")
    def payouts = column[Seq[RankType]] ("PAYOUTS")
    def wagered = column[Seq[RankType]] ("WAGERED")
    def multipliers = column[Seq[RankType]] ("MULTIPLIERS")
    def createdAt = column[Long] ("CREATED_AT")

    def * = (id,
    				profits,
    				payouts,
    				wagered,
    				multipliers,
    				createdAt) <> (RankingHistory.tupled, RankingHistory.unapply)
  }

  object Query extends TableQuery(new RankingHistoryTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}
