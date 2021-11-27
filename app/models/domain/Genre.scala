package models.domain

object Genre extends utils.CommonImplicits
case class Genre(id: java.util.UUID, name: String, description: Option[String])

