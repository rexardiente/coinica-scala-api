package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Task(id: UUID, gameID: UUID, info: JsValue, isValid: Boolean, datecreated: Long)

object Task {
	implicit def implTask = Json.format[Task]
}
