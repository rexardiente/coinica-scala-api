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
    def key = column[String] ("KEY", O.PrimaryKey)
    def game_id = column[String] ("ID")
    def owner = column[String] ("OWNER")
    def enemy = column[String] ("ENEMY")
    def time_executed = column[String] ("TIME_EXECUTED")
    def gameplay_log = column[List[String]] ("GAME_LOG")
    def isWin = column[Boolean] ("IS_WIN")

   def * = (key, game_id, owner, enemy, time_executed, gameplay_log, isWin) <> ((GQCharacterGameHistory.apply _).tupled, GQCharacterGameHistory.unapply)
  }

  object Query extends TableQuery(new GQCharacterGameHistoryTable(_)) {
    def apply(key: String) = this.withFilter(_.key === key)
  } 
}