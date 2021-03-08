package models.domain.eosio

import play.api.libs.json._

object GQGameStatus extends utils.CommonImplicits
object GQCharacterGameHistory extends utils.CommonImplicits

case class GQGameStatus(player: String, char_id: String, isWin: Boolean)
case class GQCharacterGameHistory(
    id: String, // Game ID
    winner: String,
    winnerID: String,
    loser: String,
    loserID: String,
    logs: List[GameLog],
    timeExecuted: Long) {
  def toJson(): JsValue = Json.toJson(this)
}