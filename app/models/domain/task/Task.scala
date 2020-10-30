package models.domain.task

import java.time.Instant
import java.util.UUID
import play.api.libs.json._

case class Task(id: UUID, gamename: String, taskdescription: Option[String], taskdate: Instant)

object Task {
	implicit def implTask = Json.format[Task]
}