package models.dao.ranking

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.ranking.Ranking
@Singleton
final class RankingDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  protected class RankingTable(tag: Tag) extends Table[Ranking](tag, "RANKING") {
    def id = column[UUID] ("ID")
    def rankname = column[Option[String]] ("RANKNAME")
    def rankdesc = column[String] ("RANKDESC")
    def * = (id,  rankname,  rankdesc) <> ((Ranking.apply _).tupled, Ranking.unapply) 
  }

  object Query extends TableQuery(new RankingTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}