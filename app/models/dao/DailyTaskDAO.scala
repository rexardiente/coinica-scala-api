package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.DailyTask

@Singleton
final class DailyTaskDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class DailyTaskTable(tag: Tag) extends Table[DailyTask](tag, "TASK_TRACKER") {
    def user = column[UUID] ("USER", O.PrimaryKey)
    def gameID = column[UUID] ("GAME_ID")
    // def game = column[String] ("GAME")
    def gameCount = column[Int] ("RATIO")

    def * = (user, gameID, gameCount) <> (DailyTask.tupled, DailyTask.unapply)
  }

  object Query extends TableQuery(new DailyTaskTable(_)) {
    def apply(user: UUID) = this.withFilter(_.user === user)
    def apply(user: UUID, gameID: UUID) = this.withFilter(x => x.user === user && x.gameID === gameID)
    def clearTbl = this.delete
  }
}