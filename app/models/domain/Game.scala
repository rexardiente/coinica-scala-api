package models.domain

import java.util.UUID

object Game extends utils.CommonImplicits

case class Game(id: UUID, name: String, imgURL: String, path: String, genre: UUID, description: Option[String])

