package models.domain

import java.time.Instant
import java.util.UUID
import play.api.libs.json._

case class Login(id: UUID, username: String, password: String, logincreated: Long)

object Login {
	implicit def implLogin = Json.format[Login]
}