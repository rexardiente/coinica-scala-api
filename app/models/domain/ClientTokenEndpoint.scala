package models.domain

import play.api.libs.json._

object ClientTokenEndpoint extends utils.CommonImplicits

case class ClientTokenEndpoint(id: java.util.UUID, token: String) {
	def toJson(): JsValue = Json.toJson(this)
}