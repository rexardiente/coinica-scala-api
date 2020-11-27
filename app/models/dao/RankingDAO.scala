package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.Ranking

@Singleton
final class RankingDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._


  protected class RankingTable(tag: Tag) extends Table[Ranking](tag, "RANKING") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def name = column[String] ("NAME")
    def gameID = column[UUID] ("GAME_ID") 
    def rate = column[Double] ("RATE")
    def amount = column[Double] ("AMOUNT")
    def rankingcreated = column[Long] ("RANKINGCREATED")

    def * = (id, name, gameID, rate, amount, rankingcreated) <> ((Ranking.apply _).tupled, Ranking.unapply) 
  }

  object Query extends TableQuery(new RankingTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
 
}
