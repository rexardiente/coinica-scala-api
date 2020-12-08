package models.domain.eosio

import play.api.libs.json._

object GQCharacterInfo extends utils.CommonImplicits
object GQGhost extends utils.CommonImplicits
object GQGame extends utils.CommonImplicits
object GQTable extends utils.CommonImplicits
object TableRowsResponse extends utils.CommonImplicits

case class GQCharacterInfo(
  ghost_id: String, 
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
  last_match: Long)
case class GQGhost(key: Int, value: GQCharacterInfo)
case class GQGame(character: Seq[GQGhost], monster_count: Int, summon_count: Int, status: Int)
case class GQTable(username: String, game_id: Long, game_data: GQGame) {
  def toJson(): JsValue = Json.toJson(this)
}
case class TableRowsResponse(rows: Seq[GQTable], more: Boolean, next_key: String) {
  def toJson(): JsValue = Json.toJson(this)
}