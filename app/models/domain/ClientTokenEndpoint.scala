package models.domain

import java.util.UUID
import play.api.libs.json._

object ClientTokenEndpoint extends utils.CommonImplicits

case class ClientTokenEndpoint(id: UUID, token: String) {
	def toJson(): JsValue = Json.toJson(this)
}