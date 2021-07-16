package models.domain.eosio

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

object GhostQuestCharacterValue extends utils.CommonImplicits
object GhostQuestCharacter extends utils.CommonImplicits
object GhostQuestGameData extends utils.CommonImplicits
object GhostQuestTableGameData extends utils.CommonImplicits
object GhostQuestCharacterHistory extends utils.CommonImplicits
case class GhostQuestCharacterValue(owner_id: Int,
																		ghost_name: String,
																		ghost_id: Int,
																		rarity: Int,
																		character_life: Int,
																		initial_hp: Int,
																		hitpoints: Int,
																		status: Int,
																		attack: Int,
																		defense: Int,
																		speed: Int,
																		luck: Int,
																		prize: BigDecimal,
																		battle_limit: Int,
																		battle_count: Int,
																		created_at: String,
																		last_match: Int,
																		enemy_fought: JsValue) {
	def toJson(): JsValue = Json.toJson(this)
}
case class GhostQuestCharacter(key: String, value: GhostQuestCharacterValue) {
	def toHistory(): GhostQuestCharacterHistory =
		new GhostQuestCharacterHistory(key,
																	value.owner_id,
																	value.ghost_name,
																	value.ghost_id,
																	value.rarity,
																	value.character_life,
																	value.initial_hp,
																	value.hitpoints,
																	value.status,
																	value.attack,
																	value.defense,
																	value.speed,
																	value.luck,
																	value.prize,
																	value.battle_limit,
																	value.battle_count,
																	value.created_at,
																	value.last_match,
																	value.enemy_fought)
}
case class GhostQuestGameData(characters: Seq[GhostQuestCharacter])
case class GhostQuestTableGameData(id: Int,  game_data: GhostQuestGameData)


case class GhostQuestCharacterHistory(key: String,
																			owner_id: Int,
																			ghost_name: String,
																			ghost_id: Int,
																			rarity: Int,
																			character_life: Int,
																			initial_hp: Int,
																			hitpoints: Int,
																			status: Int,
																			attack: Int,
																			defense: Int,
																			speed: Int,
																			luck: Int,
																			prize: BigDecimal,
																			battle_limit: Int,
																			battle_count: Int,
																			created_at: String,
																			last_match: Int,
																			enemy_fought: JsValue) {
	def toJson(): JsValue = Json.toJson(this)
	def toCharacterData(): GhostQuestCharacter =
		new GhostQuestCharacter(key, new GhostQuestCharacterValue(
			owner_id,
			ghost_name,
			ghost_id,
			rarity,
			character_life,
			initial_hp,
			hitpoints,
			status,
			attack,
			defense,
			speed,
			luck,
			prize,
			battle_limit,
			battle_count,
			created_at,
			last_match,
			enemy_fought))
}