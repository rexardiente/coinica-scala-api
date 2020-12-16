package models.domain.eosio

import play.api.libs.json._

object GQCharacterGameHistory extends utils.CommonImplicits

case class GQCharacterGameHistory(
    id: java.util.UUID,
    game_id: String,
    player: String,
    enemy: String,
    player_id: Long,
    enemy_id: Long,
    time_executed: Long,
    gameplay_log: List[String],
    isWin: Boolean) { // String Long value from smartcontract
  def toJson(): JsValue = Json.toJson(this)
}