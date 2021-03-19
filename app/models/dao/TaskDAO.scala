package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.Task

@Singleton
final class TaskDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider,
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] with utils.ColumnTypeImplicits {
  import profile.api._

  protected class TaskTable(tag: Tag) extends Table[Task](tag, "TASK") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def tasks = column[Seq[UUID]] ("TASKS")
    // def duration = column[Int] ("TIME_DURATION")
    def createdAt = column[Instant] ("CREATED_AT")
    // def expiredAt = column[Instant] ("EXPIRED_AT")

    def * = (id, tasks, createdAt) <> ((Task.apply _).tupled, Task.unapply)
  }

  object Query extends TableQuery(new TaskTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }

}
