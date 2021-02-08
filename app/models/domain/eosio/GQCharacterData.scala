package models.domain.eosio

import play.api.libs.json._

object GQCharacterDataTrait extends utils.CommonImplicits
object GQCharacterData
object GQCharacterDataHistory {
  val tupled = (apply: (String, String, Int, Int, Int, Int, Int, Int, Int, Int, Int, Double, Int, Int, Long, Long) => GQCharacterDataHistory).tupled
  def apply(v: GQCharacterData): GQCharacterDataHistory =
      new GQCharacterDataHistory(
          v.id,
          v.owner,
          v.character_life,
          v.initial_hp,
          v.ghost_class,
          v.ghost_level,
          v.status,
          v.attack,
          v.defense,
          v.speed,
          v.luck,
          v.prize,
          v.battle_limit,
          v.battle_count,
          v.last_match,
          v.created_at)

  def toCharacterData(v: GQCharacterDataHistory): GQCharacterData =
    new GQCharacterData(
        v.id,
        v.owner,
        v.character_life,
        v.initial_hp,
        v.ghost_class,
        v.ghost_level,
        v.status,
        v.attack,
        v.defense,
        v.speed,
        v.luck,
        v.prize,
        v.battle_limit,
        v.battle_count,
        v.last_match,
        v.created_at)
}
trait GQCharacterDataTrait {
    val id: String
    val owner: String
    val prize: Double
    val ghost_class: Int
    val ghost_level: Int
    def toJson(): JsValue = Json.toJson(this)
}
case class GQCharacterData(
    id: String,
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
    prize: Double,
    battle_limit: Int,
    battle_count: Int,
    last_match: Long,
    created_at: Long) extends GQCharacterDataTrait
case class GQCharacterDataHistory(
    id: String,
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
    prize: Double,
    battle_limit: Int,
    battle_count: Int,
    last_match: Long,
    created_at: Long) extends GQCharacterDataTrait