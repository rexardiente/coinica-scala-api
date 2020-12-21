package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.GQCharacterGameHistory

@Singleton
final class GQCharacterGameHistoryDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class GQCharacterGameHistoryTable(tag: Tag) extends Table[GQCharacterGameHistory](tag, "GQ_CHARACTER_GAME_HISTORY") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def gameID = column[String] ("GAME_ID")
    def player = column[String] ("PLAYER")
    def enemy = column[String] ("ENEMY")
    def playerID = column[String] ("PLAYER_ID")
    def enemyID = column[String] ("ENEMY_ID")
    def time_executed = column[Long] ("TIME_EXECUTED")
    def gameplay_log = column[List[String]] ("GAME_LOG")
    def isWin = column[Boolean] ("IS_WIN")

    def * = (id,
            gameID,
            player,
            enemy,
            playerID,
            enemyID,
            time_executed,
            gameplay_log,
            isWin) <> ((GQCharacterGameHistory.apply _).tupled, GQCharacterGameHistory.unapply)
  }

  object Query extends TableQuery(new GQCharacterGameHistoryTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
    def apply(id: String, player: String) = this.withFilter(x => x.gameID === id && x.player === player)
    // def apply(id: String, player: String) = this.withFilter(x => x.playerID === id && x.player === player)
  } 
}