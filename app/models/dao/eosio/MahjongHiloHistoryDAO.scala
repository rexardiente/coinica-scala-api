package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio.{ MahjongHiloHistory, MahjongHiloGameData }

@Singleton
final class MahjongHiloHistoryDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class MahjongHiloHistoryTable(tag: Tag) extends Table[MahjongHiloHistory](tag, "MAHJONG_HILO_HISTORY") {
    def gameID = column[String] ("ID", O.PrimaryKey)
    def userGameID = column[Int] ("USER_GAME_ID")
    def predictions = column[Seq[(Int, Int, Int, Int)]] ("PREDICTIONS")
    def gameData = column[Option[MahjongHiloGameData]] ("GAME_DATA")
    def status = column[Boolean] ("GAME_STATUS")

    def * = (gameID, userGameID, predictions, gameData, status) <> (MahjongHiloHistory.tupled, MahjongHiloHistory.unapply)
  }

  object Query extends TableQuery(new MahjongHiloHistoryTable(_)) {
    def apply(gameID: String) = this.withFilter(_.gameID === gameID)
  }
}