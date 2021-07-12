package models.domain.eosio

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

object GhostQuestGameData extends utils.CommonImplicits
object GhostQuestTableGameData extends utils.CommonImplicits
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
																		last_match: Int,
																		enemy_fought: JsValue)
case class GhostQuestCharacters(key: String, value: GhostQuestCharacterValue)
case class GhostQuestGameData(characters: Seq[GhostQuestCharacters])
case class GhostQuestTableGameData(id: Int,  game_data: GhostQuestGameData)

// {
//    "id":2,
//    "game_data":{
//       "characters":[
//          {
//             "key":"9d7da2cb1aad5cb8db8c2e431e56e10",
//             "value":{
//                "owner_id":2,
//                "ghost_name":"Nureonnna",
//                "ghost_id":104,
//                "rarity":5,
//                "character_life":1,
//                "initial_hp":0,
//                "hitpoints":137,
//                "status":1,
//                "attack":63,
//                "defense":61,
//                "speed":67,
//                "luck":66,
//                "prize":"1.00000000000000000",
//                "battle_limit":10,
//                "battle_count":0,
//                "last_match":0,
//                "enemy_fought":[Map(Int, String)]
//             }
//          }
//       ]
//    }
// }