package models.dao.task

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.task.Task

@Singleton
final class TaskDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  protected class TaskTable(tag: Tag) extends Table[Task](tag, "TASK") {
    def id = column[UUID] ("ID")
    def gamename = column[String] ("GAMENAME") 
    def taskdescription = column[Option[String]] ("TASKDESCRIPTION")
    def taskdate =column[Instant] ("TASKDATE")

    def * = (id, gamename,  taskdescription, taskdate) <> ((Task.apply _).tupled, Task.unapply) 
  }

  object Query extends TableQuery(new TaskTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}
