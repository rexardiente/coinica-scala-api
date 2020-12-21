package models.domain.eosio

import play.api.libs.json._

object GQCharacterDataHistory extends utils.CommonImplicits {
  def fromCharacterData(data: GQCharacterData): GQCharacterDataHistory =
      new GQCharacterDataHistory(
          data.id,
          data.owner,
          data.character_life,
          data.initial_hp,
          data.hitpoints,
          data.ghost_class,
          data.ghost_level,
          data.status,
          data.attack,
          data.defense,
          data.speed,
          data.luck,
          data.prize,
          data.battle_limit,
          data.battle_count,
          data.last_match)
}

case class GQCharacterDataHistory(
    id: String,
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
    last_match: Long) {
    def toJson(): JsValue = Json.toJson(this)
}