package models.domain.user.info

import java.time.Instant
import java.util.UUID
import play.api.libs.json._

case class User(id: UUID, account: String, address: String, createdAt: Instant)

object User {
	implicit def implUser = Json.format[User]
}
