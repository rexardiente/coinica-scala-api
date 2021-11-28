package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.{ Task, DailyTask, TaskHistory, TaskGameInfo }

@Singleton
final class TaskDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class TaskTable(tag: Tag) extends Table[Task](tag, "TASK") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def tasks = column[Seq[TaskGameInfo]] ("TASKS")
    def createdAt = column[Long] ("CREATED_AT")

    def * = (id, tasks, createdAt) <> ((Task.apply _).tupled, Task.unapply)
  }
  protected class DailyTaskTable(tag: Tag) extends Table[DailyTask](tag, "TASK_TRACKER") {
    def id = column[UUID] ("ID")
    def user = column[UUID] ("USER")
    def gameID = column[UUID] ("GAME_ID")
    def gameCount = column[Int] ("RATIO")

    def * = (id, user, gameID, gameCount) <> (DailyTask.tupled, DailyTask.unapply)
    def fk = foreignKey("TASK", id, Query)(_.id)
  }
  protected class TaskHistoryTable(tag: Tag) extends Table[TaskHistory](tag, "TASK_HISTORY") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def tasks = column[List[DailyTask]]("TASKS")
    def validAt = column[Instant] ("VALID_AT")
    def expiredAt = column[Instant] ("EXPIRED_AT")

    def * = (id, tasks, validAt, expiredAt) <> ((TaskHistory.apply _).tupled, TaskHistory.unapply)
    def fk = foreignKey("TASK", id, Query)(_.id)
  }

  object Query extends TableQuery(new TaskTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
    def apply(createdAt: Long) = this.withFilter(_.createdAt === createdAt)
  }
  object DailyTaskQuery extends TableQuery(new DailyTaskTable(_)) {
    def apply(user: UUID) = this.withFilter(_.user === user)
    def apply(user: UUID, gameID: UUID) = this.withFilter(x => x.user === user && x.gameID === gameID)
    def clearTbl = this.delete
  }
  object TaskHistoryQuery extends TableQuery(new TaskHistoryTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }

}
