package models.domain.eosio

import play.api.libs.json._

object GQCharacterPrevMatch extends utils.CommonImplicits
object GQCharacterInfo extends utils.CommonImplicits
object GQGhost extends utils.CommonImplicits
object GQGame extends utils.CommonImplicits
object GQTable extends utils.CommonImplicits
object GQRowsResponse extends utils.CommonImplicits

case class GQCharacterPrevMatch(key: Int, value: String)
case class GQCharacterInfo(
  owner: String, 
  character_life: Int,
  initial_hp: Int, 
  hitpoints: Int, 
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
  enemy_fought: Seq[GQCharacterPrevMatch])
case class GQGhost(key: Long, value: GQCharacterInfo)
case class GQGame(character: Seq[GQGhost], status: Int)
case class GQTable(username: String, game_id: Long, game_data: GQGame) {
  def toJson(): JsValue = Json.toJson(this)
}
case class GQRowsResponse(rows: Seq[GQTable], more: Boolean, next_key: String) {
  def toJson(): JsValue = Json.toJson(this)
}

// uuid // auto generated as primary key 
// GQGhost key // not morethan 1
// username
// GQCharacterInfo // enemy_fought must have game_id
