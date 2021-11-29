package models.domain

import java.util.UUID
import java.time.Instant
import models.domain.enum.Roles

object Admin extends utils.CommonImplicits {
	val tupled = (apply: (UUID, String, Roles.Value, Instant) => Admin).tupled
	def apply(email: String, role: Roles.Value): Admin =
    Admin(UUID.randomUUID, email, role, utils.SystemConfig.instantNowUTC())
}

case class Admin(id: UUID, email: String, role: Roles.Value, createdAt: Instant)