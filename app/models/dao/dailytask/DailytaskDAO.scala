package models.dao.dailytask

import java.util.UUID
import java.time.Instant
import javax.inject.{ Inject, Singleton }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.dailytask.Dailytask

@Singleton
final class DailytaskDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  protected class DailytaskTable(tag: Tag) extends Table[Dailytask](tag, "DAILYTASK") {
    def id = column[UUID] ("ID")
    def gamename = column[String] ("GAMENAME") 
    def taskdescription = column[Option[String]] ("TASKDESCRIPTION")
    def taskdate =column[Instant] ("TASKDATE")

    def * = (id, gamename,  taskdescription, taskdate) <> ((Dailytask.apply _).tupled, Dailytask.unapply) 
  }

  object Query extends TableQuery(new DailytaskTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
}
