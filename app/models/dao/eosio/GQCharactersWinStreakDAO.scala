package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.Future
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.eosio._

@Singleton
final class GQCharactersLifeTimeWinStreakDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class GQCharactersLifeTimeWinStreakTable(tag: Tag) extends Table[GQCharactersLifeTimeWinStreak](tag, "GQ_CHARACTER_WIN_STREAK_HISTORY") {
    def id = column[String] ("ID", O.PrimaryKey)
    def current_win_streak = column[List[String]] ("CURRENT_WIN_STREAK")
    def highest_win_streak = column[List[String]] ("HIGHEST_WIN_STREAK")
    def updated_at = column[Long] ("UPDATED_AT")
    def created_at = column[Long] ("CREATED_AT")

    def * = (id,
            current_win_streak,
            highest_win_streak,
            updated_at,
            created_at) <> ((GQCharactersLifeTimeWinStreak.apply _).tupled, GQCharactersLifeTimeWinStreak.unapply)
  }

  object Query extends TableQuery(new GQCharactersLifeTimeWinStreakTable(_)) {
    def apply(id: String) = this.withFilter(_.id === id)
  }
}