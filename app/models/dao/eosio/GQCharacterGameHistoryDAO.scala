package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio._

@Singleton
final class GQCharacterGameHistoryDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class GQCharacterGameHistoryTable(tag: Tag) extends Table[GQCharacterGameHistory](tag, "GQ_CHARACTER_GAME_HISTORY") {
    def id = column[String] ("GAME_ID", O.PrimaryKey)
    def txHash = column[String] ("TX_HASH")
    def winner = column[UUID] ("PLAYER_1")
    def winnerID = column[String] ("PLAYER_1_ID")
    def loser = column[UUID] ("PLAYER_2")
    def loserID = column[String] ("PLAYER_2_ID")
    def log = column[List[GameLog]] ("GAME_LOG")
    def timeExecuted = column[Long] ("TIME_EXECUTED")

    def * = (id,
            txHash,
            winner,
            winnerID,
            loser,
            loserID,
            log,
            timeExecuted) <> ((GQCharacterGameHistory.apply _).tupled, GQCharacterGameHistory.unapply)
  }

  object Query extends TableQuery(new GQCharacterGameHistoryTable(_)) {
    def apply(id: String) = this.withFilter(_.id === id)
    // def apply(id: String, player: String) = this.withFilter(x => x.id === id && x.player === player)
    // def apply(id: String, player: String) = this.withFilter(x => x.playerID === id && x.player === player)
  }
}