package models.domain.eosio

import play.api.libs.json._

object GQCharactersRank extends utils.CommonImplicits

case class GQCharactersRank(id: String,
														owner: String,
														ghost_class: Int,
														ghost_level: Int,
														earned: Double) {
	def toJson(): JsValue = Json.toJson(this)
}

