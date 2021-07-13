package models.dao

import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio._

@Singleton
final class GhostQuestCharacterGameHistoryDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class GhostQuestCharacterGameHistoryTable(tag: Tag) extends Table[GhostQuestCharacterGameHistory](tag, "GHOST_QUEST_CHARACTER_GAME_HISTORY") {
    def id = column[String] ("GAME_ID", O.PrimaryKey)
    def txHash = column[String] ("TX_HASH")
    def winner = column[Int] ("WINNER")
    def winnerID = column[String] ("WINNER_ID")
    def loser = column[Int] ("LOSER")
    def loserID = column[String] ("LOSER_ID")
    def log = column[List[GhostQuestCharacterGameLog]] ("GAME_LOG")
    def timeExecuted = column[Long] ("TIME_EXECUTED")

    def * = (id,
            txHash,
            winner,
            winnerID,
            loser,
            loserID,
            log,
            timeExecuted) <> ((GhostQuestCharacterGameHistory.apply _).tupled, GhostQuestCharacterGameHistory.unapply)
  }

  object Query extends TableQuery(new GhostQuestCharacterGameHistoryTable(_)) {
    def apply(id: String) = this.withFilter(_.id === id)
    // def apply(id: String, player: String) = this.withFilter(x => x.id === id && x.player === player)
    // def apply(id: String, player: String) = this.withFilter(x => x.playerID === id && x.player === player)
  }
}