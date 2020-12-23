package models.domain.eosio

import play.api.libs.json._

object GQCharacterPrevMatchData extends utils.CommonImplicits
object GQCharacterPrevMatch extends utils.CommonImplicits
object GQCharacterInfo extends utils.CommonImplicits
object GQGhost extends utils.CommonImplicits
object GQGame extends utils.CommonImplicits
object GQTable extends utils.CommonImplicits
object GQRowsResponse extends utils.CommonImplicits
case class GQCharacterPrevMatchData(
enemy: String,
enemy_id: String,
time_executed: Long,
gameplay_log: List[String],
isWin: Boolean)
case class GQCharacterPrevMatch(key: String, value: GQCharacterPrevMatchData)
case class GQCharacterInfo(
  owner: String, 
  character_life: Int,
  initial_hp: Int, 
  ghost_class: Int, 
  ghost_level: Int, 
  status: Int, 
  attack: Int, 
  defense: Int, 
  speed: Int, 
  luck: Int, 
  prize: String, 
  battle_limit: Int, 
  battle_count: Int, 
  last_match: Long, // String Long value from smartcontract
  match_history: Seq[GQCharacterPrevMatch])
case class GQGhost(key: String, value: GQCharacterInfo)
case class GQGame(character: Seq[GQGhost])
case class GQTable(username: String, game_data: GQGame) {
  def toJson(): JsValue = Json.toJson(this)
}
case class GQRowsResponse(rows: Seq[GQTable], more: Boolean, next_key: String) {
  def toJson(): JsValue = Json.toJson(this)
}
