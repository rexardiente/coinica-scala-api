package models.domain.eosio

import play.api.libs.json._

object GQGhost
object GQGame
object GQTable extends utils.CommonImplicits
object TableRowsResponse extends utils.CommonImplicits

case class GQGhost(
  ghost_id: String, 
  key: Int, 
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
case class GQGame(character: Seq[GQGhost], monster_count: Int, summon_count: Int, status: Int)
case class GQTable(username: String, game_id: Long, game_data: GQGame) {
  def toJson(): JsValue = Json.toJson(this)
}
case class TableRowsResponse(rows: Seq[GQTable], more: Boolean, next_key: String) {
  def toJson(): JsValue = Json.toJson(this)
}