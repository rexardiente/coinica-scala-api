package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.TaskHistory

@Singleton
final class TaskHistoryDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class TaskHistoryTable(tag: Tag) extends Table[TaskHistory](tag, "TASK_HISTORY") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def taskID = column[UUID] ("TASKS_ID")
    def gameID = column[UUID] ("GAME_ID")
    def user = column[UUID] ("USER")
    def gameCount = column[Int] ("GAME_COUNT")
    def createdAt = column[Instant] ("CREATED_AT")
    def expiredAt = column[Instant] ("EXPIRED_AT")

    def * = (id,
    				taskID,
    				gameID,
    				user,
    				gameCount,
    				createdAt,
    				expiredAt) <> ((TaskHistory.apply _).tupled, TaskHistory.unapply)
  }

  object Query extends TableQuery(new TaskHistoryTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}
