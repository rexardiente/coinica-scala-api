package models.domain.eosio

import play.api.libs.json._

object GQCharacterDataHistoryLogs extends utils.CommonImplicits

case class GQCharacterDataHistoryLogs(gameID: String, isWin: List[GQGameStatus], timeExecuted: Long, logs: List[String]) {
	def toJson(): JsValue = Json.toJson(this)
}