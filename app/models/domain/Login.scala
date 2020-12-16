package models.domain.login

import java.time.Instant
import java.util.UUID
import play.api.libs.json._

case class Login(id: UUID, emailaddress: String, password: String, logincreated: Instant)

object Login {
	implicit def implLogin = Json.format[Login]
}