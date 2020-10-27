package models.domain.dailytask

import java.time.Instant
import java.util.UUID
import play.api.libs.json._

case class Dailytask(id: UUID, gamename: String, taskdescription: Option[String])

object Dailytask {
	implicit def implDailytask = Json.format[Dailytask]
}