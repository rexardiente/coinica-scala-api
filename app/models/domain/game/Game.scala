package models.domain.game

import java.util.UUID
import play.api.libs.json._

case class Game(id: UUID, name: String, description: Option[String])

object Game {
	implicit def implGame = Json.format[Game]
}
