package models.domain.eosio

import play.api.libs.json._

object GQCharacterGameHistory extends utils.CommonImplicits

case class GQCharacterGameHistory(
    key: String,
    game_id: String,
    owner: String,
    enemy: String,
    time_executed: String,
    gameplay_log: List[String],
    isWin: Boolean) { // String Long value from smartcontract
  def toJson(): JsValue = Json.toJson(this)
}