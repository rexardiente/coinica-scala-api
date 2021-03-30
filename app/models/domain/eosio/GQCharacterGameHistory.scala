package models.domain.eosio

import java.util.UUID
import play.api.libs.json._

object GQGameStatus extends utils.CommonImplicits
object GQCharacterGameHistory extends utils.CommonImplicits

case class GQGameStatus(player: UUID, char_id: String, isWin: Boolean)
case class GQCharacterGameHistory(
    id: String, // Game ID
    txHash: String, // Game ID
    winner: UUID,
    winnerID: String,
    loser: UUID,
    loserID: String,
    logs: List[GameLog],
    timeExecuted: Long) {
  def toJson(): JsValue = Json.toJson(this)
}