package models.domain.eosio

import play.api.libs.json._

object GQGameStatus extends utils.CommonImplicits
object GQCharacterGameHistory extends utils.CommonImplicits

case class GQGameStatus(player: String, isWin: Boolean)
case class GQCharacterGameHistory(
    id: String,
    player1: String,
    player1ID: String,
    player2: String,
    player2ID: String,
    timeExecuted: Long,
    log: List[String],
    status: List[GQGameStatus]) {
  def toJson(): JsValue = Json.toJson(this)
}