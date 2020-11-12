package models.dao

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import models.domain.Task

@Singleton
final class TaskDAO @Inject()(
    protected val dbConfigProvider: DatabaseConfigProvider
  ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
  import profile.api._

  private implicit val jsValueMappedColumnType: BaseColumnType[JsValue] =
    MappedColumnType.base[JsValue, String](Json.stringify, Json.parse)

  protected class TaskTable(tag: Tag) extends Table[Task](tag, "TASK") {
    def id = column[UUID] ("ID", O.PrimaryKey)
    def gameID = column[UUID] ("GAME_ID") 
    def info = column[JsValue] ("INFO")
    def isValid = column[Boolean] ("IS_VALID")
    def datecreated = column[Instant] ("DATECREATED")

    def * = (id, gameID, info, isValid, datecreated) <> ((Task.apply _).tupled, Task.unapply) 
  }

  object Query extends TableQuery(new TaskTable(_)) {
    def apply(id: UUID) = this.withFilter(_.id === id)
  }
 
}
