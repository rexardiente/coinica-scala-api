package models.domain.eosio

import play.api.libs.json._

object GQCharacterDataHistory extends utils.CommonImplicits

case class GQCharacterDataHistory(
    id: java.util.UUID,
    gameID: Long,
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
    last_match: Long,
    matches: Int) {
  def toJson(): JsValue = Json.toJson(this)
}