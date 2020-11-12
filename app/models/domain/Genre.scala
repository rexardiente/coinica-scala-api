package models.domain


case class Genre(id: java.util.UUID, name: String, description: Option[String])

object Genre extends utils.CommonImplicits 