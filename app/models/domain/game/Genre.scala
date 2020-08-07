package models.domain.game

import java.util.UUID
import play.api.libs.json._

case class Genre(id: UUID, name: String, description: Option[String])

object Genre {
	implicit def implGenre = Json.format[Genre]
}
