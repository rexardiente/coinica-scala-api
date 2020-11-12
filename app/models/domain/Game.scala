package models.domain

import java.util.UUID
import play.api.libs.json._

case class Game(id: UUID, name: String, imgURL: String, path: String, genre: UUID, description: Option[String])

object Game {
	implicit def implGame = Json.format[Game]
}
