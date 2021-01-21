package models.domain

import java.util.UUID
import java.time.Instant

object Admin extends utils.CommonImplicits {
	val tupled = (apply: (UUID, String, Roles.Value, Instant) => Admin).tupled
	def apply(email: String, role: Roles.Value): Admin =
    Admin(UUID.randomUUID, email, role, Instant.now)
}

case class Admin(id: UUID, email: String, role: Roles.Value, createdAt: Instant)