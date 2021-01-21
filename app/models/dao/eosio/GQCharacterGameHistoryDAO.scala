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
    def player1 = column[String] ("PLAYER_1")
    def player1ID = column[String] ("PLAYER_1_ID")
    def player2 = column[String] ("PLAYER_2")
    def player2ID = column[String] ("PLAYER_2_ID")
    def timeExecuted = column[Long] ("TIME_EXECUTED")
    def log = column[List[String]] ("GAME_LOG")
    def status = column[List[GQGameStatus]] ("STATUS")

    def * = (id,
            player1,
            player1ID,
            player2,
            player2ID,
            timeExecuted,
            log,
            status) <> ((GQCharacterGameHistory.apply _).tupled, GQCharacterGameHistory.unapply)
  }

  object Query extends TableQuery(new GQCharacterGameHistoryTable(_)) {
    def apply(id: String) = this.withFilter(_.id === id)
    // def apply(id: String, player: String) = this.withFilter(x => x.id === id && x.player === player)
    // def apply(id: String, player: String) = this.withFilter(x => x.playerID === id && x.player === player)
  }
}