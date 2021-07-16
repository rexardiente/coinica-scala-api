package models.domain.eosio

import play.api.libs.json._

object GhostQuestCharacterGameLog extends utils.CommonImplicits
object GhostQuestCharacterGameHistory extends utils.CommonImplicits
object GhostQuestBattleResult extends utils.CommonImplicits
case class GhostQuestCharacterGameLog(round: Int, attacker: Int, defender: Int, damage: Int, is_crit: Boolean)
case class GhostQuestCharacterGameHistory(id: String,
                                          txHash: String,
                                          winner: Int,
                                          winnerID: String,
                                          loser: Int,
                                          loserID: String,
                                          logs: List[GhostQuestCharacterGameLog],
                                          timeExecuted: Long) {
  def toJson(): JsValue = Json.toJson(this)
}
case class GhostQuestBattleResult(id: java.util.UUID, characters: Map[String, (Int, Boolean)], logs: List[GhostQuestCharacterGameLog])