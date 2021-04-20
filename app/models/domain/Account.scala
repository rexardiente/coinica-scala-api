package models.domain

import java.time.Instant
import java.util.UUID
import play.api.libs.json._

object Account {
	implicit def implAccount = Json.format[Account]
}

case class Account(id: UUID, username: String, password: Option[String], createdAt: Long) {
	def toJson(): JsValue = Json.toJson(this)
}
