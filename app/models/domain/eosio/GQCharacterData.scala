package models.domain.eosio

import play.api.libs.json._

object GQCharacterData extends utils.CommonImplicits

case class GQCharacterData(
    characterID: String,
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
    last_match: Long) { // String Long value from smartcontract
  def toJson(): JsValue = Json.toJson(this)
}