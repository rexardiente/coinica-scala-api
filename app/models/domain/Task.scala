package models.domain

import java.time.Instant
import java.util.UUID
import play.api.libs.json._

case class Task(id: UUID, gameID: UUID, info: JsValue, isValid: Boolean, datecreated: Instant)

object Task {
	implicit def implTask = Json.format[Task]
}
